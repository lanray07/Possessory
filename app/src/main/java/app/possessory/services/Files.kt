package app.possessory.services

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.room.withTransaction
import app.possessory.InventoryState
import app.possessory.PossessoryApp
import app.possessory.data.*
import app.possessory.domain.InventoryRules
import app.possessory.settings
import com.google.gson.Gson
import java.io.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Currency
import java.util.UUID
import java.util.zip.*
import javax.crypto.*
import javax.crypto.spec.*
import kotlinx.coroutines.flow.first

data class Snapshot(
    val schema: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    val households: List<Household>,
    val rooms: List<RoomRecord>,
    val items: List<InventoryItem>,
    val attachments: List<Attachment>,
    val maintenance: List<Maintenance>,
    val hashes: Map<String, String>,
    val extras: Map<String, String> = emptyMap(),
)

object Files {
    private const val LIMIT = 100L * 1024 * 1024
    private const val MANIFEST_LIMIT = 8L * 1024 * 1024
    val gson = Gson()

    fun readBounded(input: InputStream, max: Long = LIMIT): ByteArray =
        ByteArrayOutputStream().also { copyBounded(input, it, max) }.toByteArray()

    fun hash(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun importAttachment(context: Context, uri: Uri, item: String, kind: String): Attachment {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        require(mime in listOf("image/jpeg", "image/png", "image/webp", "application/pdf")) {
            "unsupported_file"
        }
        val id = UUID.randomUUID().toString()
        val file = File(context.filesDir, "attachments/$id").apply { parentFile!!.mkdirs() }
        try {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                file.outputStream().use { output -> copyBounded(input, output, 25L * 1024 * 1024) }
            }
            return Attachment(
                id = id,
                itemId = item,
                name = "$kind ${java.time.LocalDate.now()}",
                mime = mime,
                file = id,
                kind = kind,
            )
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    private fun copyBounded(input: InputStream, output: OutputStream, max: Long) {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= max) { "file_too_large" }
            output.write(buffer, 0, count)
        }
    }

    fun backup(
        context: Context,
        s: InventoryState,
        output: OutputStream,
        extras: Map<String, String> = emptyMap(),
    ) {
        val files =
            (s.attachments.map { it.file } +
                    s.rooms.map { it.photoFile }.filter { it.isNotBlank() })
                .associateWith { File(context.filesDir, "attachments/$it") }
        require(files.values.all { it.isFile }) { "missing_attachment" }
        require(files.values.sumOf { it.length() } <= LIMIT - 1024 * 1024) { "backup_too_large" }
        val snapshot =
            Snapshot(
                households = s.households,
                rooms = s.rooms,
                items = s.items,
                attachments = s.attachments,
                maintenance = s.maintenance,
                hashes = files.mapValues { hash(it.value) },
                extras = extras,
            )
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            val manifest = gson.toJson(snapshot).toByteArray(Charsets.UTF_8)
            require(manifest.size <= MANIFEST_LIMIT) { "backup_too_large" }
            require(manifest.size + files.values.sumOf { it.length() } <= LIMIT) {
                "backup_too_large"
            }
            zip.write(manifest)
            zip.closeEntry()
            files.forEach { (name, file) ->
                zip.putNextEntry(ZipEntry("attachments/$name"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    data class Preview(
        val snapshot: Snapshot,
        val files: Map<String, ByteArray>,
        val stagedFiles: Map<String, File> = emptyMap(),
        private val stagingDirectory: File? = null,
    ) {
        fun copy(name: String, destination: File) {
            stagedFiles[name]?.inputStream()?.use { input ->
                destination.outputStream().use { input.copyTo(it) }
            } ?: destination.writeBytes(files.getValue(name))
        }

        fun close() {
            stagingDirectory?.deleteRecursively()
        }
    }

    fun preview(input: InputStream, cacheDirectory: File? = null): Preview {
        val stage =
            cacheDirectory?.let {
                File(it, "backup-previews/" + UUID.randomUUID()).apply { mkdirs() }
            }
        return try {
            previewInternal(input, stage)
        } catch (error: Exception) {
            stage?.deleteRecursively()
            throw error
        }
    }

    private fun previewInternal(input: InputStream, stage: File?): Preview {
        val entries = mutableMapOf<String, ByteArray>()
        val staged = mutableMapOf<String, File>()
        var total = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(
                    entries.size < 10000 &&
                        !entry.isDirectory &&
                        (entry.name == "manifest.json" ||
                            Regex("attachments/[a-zA-Z0-9-]{1,80}").matches(entry.name))
                ) {
                    "invalid_backup"
                }
                require(!entries.containsKey(entry.name)) { "invalid_backup" }
                val maximum =
                    minOf(
                        LIMIT - total,
                        if (entry.name == "manifest.json") MANIFEST_LIMIT else LIMIT,
                    )
                if (stage == null) {
                    val output = ByteArrayOutputStream()
                    copyBounded(zip, output, maximum)
                    val bytes = output.toByteArray()
                    total += bytes.size
                    entries[entry.name] = bytes
                } else {
                    val file = File(stage, UUID.randomUUID().toString())
                    file.outputStream().use { copyBounded(zip, it, maximum) }
                    total += file.length()
                    staged[entry.name] = file
                    entries[entry.name] = ByteArray(0)
                }
            }
        }
        val manifest =
            staged.remove("manifest.json")?.readBytes()
                ?: entries["manifest.json"]
                ?: error("invalid_backup")
        entries.remove("manifest.json")
        fun entryHash(name: String) = staged[name]?.let { hash(it) } ?: hash(entries.getValue(name))
        val tree =
            com.google.gson.JsonParser.parseString(String(manifest, Charsets.UTF_8)).asJsonObject
        require(tree.get("schema")?.asInt in 1..2) { "unsupported_schema" }
        if (!tree.has("extras")) tree.add("extras", com.google.gson.JsonObject())
        // Gson does not apply Kotlin defaults to missing fields in v1 backups.
        mapOf(
                "rooms" to listOf("photoFile"),
                "items" to listOf("reminderOffsets", "reminderDates"),
                "attachments" to listOf("maintenanceId"),
            )
            .forEach { (group, fields) ->
                tree.getAsJsonArray(group).forEach { element ->
                    fields.forEach { key ->
                        if (!element.asJsonObject.has(key))
                            element.asJsonObject.addProperty(key, "")
                    }
                }
            }
        val s = gson.fromJson(tree, Snapshot::class.java)
        require(s.extras.keys.all { it == "saved_searches" })
        s.extras["saved_searches"]?.let { text ->
            require(text.length <= 65536)
            val searches = gson.fromJson(text, Array<app.possessory.domain.SavedSearch>::class.java)
            require(searches.size <= 20)
            searches.forEach {
                require(it.name.isNotBlank() && it.name.length <= 200)
                it.criteria.validate()
            }
        }
        require(s.items.size <= 100000) { "unsupported_schema" }
        fun unique(ids: List<String>) =
            ids.all { Regex("[a-zA-Z0-9-]{1,80}").matches(it) } && ids.distinct().size == ids.size
        require(
            unique(s.households.map { it.id }) &&
                unique(s.rooms.map { it.id }) &&
                unique(s.items.map { it.id }) &&
                unique(s.attachments.map { it.id }) &&
                unique(s.maintenance.map { it.id })
        ) {
            "invalid_backup"
        }
        require(
            s.households.all {
                it.name.isNotBlank() && runCatching { Currency.getInstance(it.currency) }.isSuccess
            }
        )
        require(
            s.rooms.all { room ->
                room.name.isNotBlank() && s.households.any { it.id == room.householdId }
            }
        )
        require(
            s.items.all { item ->
                item.name.isNotBlank() &&
                    s.rooms.any { it.id == item.roomId } &&
                    runCatching { Currency.getInstance(item.currency) }.isSuccess &&
                    (item.priceMinor ?: 0) >= 0 &&
                    (item.estimateMinor ?: 0) >= 0 &&
                    InventoryRules.validDate(item.purchaseDate) &&
                    InventoryRules.validDate(item.warrantyEnd) &&
                    InventoryRules.validDate(item.warrantyStart) &&
                    item.reminderDays in 0..365 &&
                    runCatching {
                            app.possessory.domain.ReminderRules.offsets(item.reminderOffsets)
                            app.possessory.domain.ReminderRules.dates(item.reminderDates)
                        }
                        .isSuccess
            }
        )
        require(
            s.maintenance.all { m ->
                s.items.any { it.id == m.itemId } &&
                    m.type.isNotBlank() &&
                    InventoryRules.validDate(m.date) &&
                    InventoryRules.validDate(m.nextDate) &&
                    m.repeatDays in 0..3650
            }
        )
        val names =
            s.attachments.map { it.file } + s.rooms.map { it.photoFile }.filter { it.isNotBlank() }
        require(names.distinct().size == names.size && unique(names))
        require(entries.keys == names.map { "attachments/$it" }.toSet())
        require(s.hashes.keys == names.toSet())
        names.forEach { name ->
            require(s.hashes[name] == entryHash("attachments/$name")) { "integrity_failed" }
        }
        s.rooms
            .filter { it.photoFile.isNotBlank() }
            .forEach { room ->
                val options =
                    android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val stagedPhoto = staged["attachments/${room.photoFile}"]
                if (stagedPhoto != null)
                    android.graphics.BitmapFactory.decodeFile(stagedPhoto.path, options)
                else {
                    val bytes = entries.getValue("attachments/${room.photoFile}")
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                }
                require(options.outWidth > 0 && options.outHeight > 0) { "unsupported_file" }
            }
        s.attachments.forEach { a ->
            require(
                s.items.any { it.id == a.itemId } &&
                    a.mime in listOf("image/jpeg", "image/png", "image/webp", "application/pdf")
            )
            require(
                a.maintenanceId.isBlank() ||
                    s.maintenance.any { it.id == a.maintenanceId && it.itemId == a.itemId }
            )
            require(s.hashes[a.file] == entryHash("attachments/${a.file}")) { "integrity_failed" }
        }
        return Preview(s, if (stage == null) entries else emptyMap(), staged, stage)
    }

    suspend fun restore(app: PossessoryApp, preview: Preview) {
        val s = preview.snapshot
        val dao = app.db.inventory()
        // Merge by stable IDs; existing records win. Remap imported file names so no existing file
        // is
        // overwritten.
        val current = dao.attachments().first()
        val existingItems = dao.items().first().map { it.id }.toSet()
        val existingRooms = dao.rooms().first().map { it.id }.toSet()
        val files = mutableListOf<File>()
        var committed = false
        try {
            val imported =
                s.attachments
                    .filter { a -> current.none { it.id == a.id } && a.itemId !in existingItems }
                    .map { a ->
                        val name = UUID.randomUUID().toString()
                        val file =
                            File(app.filesDir, "attachments/$name").apply { parentFile!!.mkdirs() }
                        files.add(file)
                        preview.copy("attachments/${a.file}", file)
                        a.copy(file = name)
                    }
            val importedRooms =
                s.rooms
                    .filter { it.id !in existingRooms }
                    .map { room ->
                        if (room.photoFile.isBlank()) room
                        else {
                            val name = UUID.randomUUID().toString()
                            val file =
                                File(app.filesDir, "attachments/$name").apply {
                                    parentFile!!.mkdirs()
                                }
                            files.add(file)
                            preview.copy("attachments/${room.photoFile}", file)
                            room.copy(photoFile = name)
                        }
                    }
            app.db.withTransaction {
                val homes = dao.households().first().map { it.id }.toSet()
                val rooms = dao.rooms().first().map { it.id }.toSet()
                val maintenance = dao.maintenance().first().map { it.id }.toSet()
                s.households.filter { it.id !in homes }.forEach { dao.put(it) }
                importedRooms.forEach { dao.put(it) }
                s.items.filter { it.id !in existingItems }.forEach { dao.put(it) }
                imported.forEach { dao.put(it) }
                s.maintenance
                    .filter { it.id !in maintenance && it.itemId !in existingItems }
                    .forEach { dao.put(it) }
            }
            committed = true
            s.extras["saved_searches"]?.let { incoming ->
                val key = androidx.datastore.preferences.core.stringPreferencesKey("saved_searches")
                app.settings.edit { prefs ->
                    val previous =
                        gson
                            .fromJson(
                                prefs[key] ?: "[]",
                                Array<app.possessory.domain.SavedSearch>::class.java,
                            )
                            .toList()
                    val imported =
                        gson
                            .fromJson(
                                incoming,
                                Array<app.possessory.domain.SavedSearch>::class.java,
                            )
                            .toList()
                    prefs[key] =
                        gson.toJson(
                            (previous +
                                    imported.filter { candidate ->
                                        previous.none { it.name == candidate.name }
                                    })
                                .take(20)
                        )
                }
            }
        } catch (e: Exception) {
            if (!committed) files.forEach { it.delete() }
            throw e
        }
    }

    fun csv(
        items: List<InventoryItem>,
        rooms: List<RoomRecord>,
        serials: Boolean,
        prices: Boolean,
        context: Context? = null,
        attachments: List<Attachment> = emptyList(),
    ): String =
        "\uFEFF" +
            (listOf(
                    (if (context == null)
                        listOf(
                            "Item",
                            "Room",
                            "Category",
                            "Purchase date",
                            "Purchase amount",
                            "Currency",
                            "Serial",
                            "Retailer",
                            "Warranty expiry",
                        )
                    else
                        listOf(
                                app.possessory.R.string.column_item,
                                app.possessory.R.string.column_room,
                                app.possessory.R.string.column_category,
                                app.possessory.R.string.column_purchase_date,
                                app.possessory.R.string.column_purchase_amount,
                                app.possessory.R.string.column_currency,
                                app.possessory.R.string.column_serial,
                                app.possessory.R.string.column_retailer,
                                app.possessory.R.string.column_warranty,
                            )
                            .map { context.getString(it) }) +
                        (if (attachments.isNotEmpty())
                            listOf(
                                context?.getString(app.possessory.R.string.receipt_reference)
                                    ?: "Attachment references"
                            )
                        else emptyList())
                ) +
                    items.map { i ->
                        listOf(
                            i.name,
                            rooms.find { it.id == i.roomId }?.name ?: "",
                            i.category,
                            i.purchaseDate,
                            if (prices)
                                InventoryRules.editableAmount(
                                    i.priceMinor,
                                    i.currency,
                                    java.util.Locale.ROOT,
                                )
                            else "",
                            i.currency,
                            if (serials) i.serial else "",
                            i.retailer,
                            i.warrantyEnd,
                        ) +
                            (if (attachments.isNotEmpty())
                                listOf(
                                    attachments
                                        .filter { it.itemId == i.id && it.kind != "Photo" }
                                        .joinToString("; ") { it.name + " " + it.id }
                                )
                            else emptyList())
                    })
                .joinToString("\r\n") { row -> row.joinToString(",") { InventoryRules.csv(it) } }

    fun pdf(
        context: Context,
        items: List<InventoryItem>,
        rooms: List<RoomRecord>,
        serials: Boolean,
        prices: Boolean,
        output: OutputStream,
    ) {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNum = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun text(
            value: String,
            x: Float,
            baseline: Float,
            size: Float,
            color: Int = Color.rgb(36, 52, 74),
        ) {
            paint.color = color
            paint.textSize = size
            page!!.canvas.drawText(value, x, baseline, paint)
        }
        fun newPage() {
            page?.let { document.finishPage(it) }
            pageNum++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
            y = 136f
            text("POSSESSORY", 40f, 50f, 24f)
            text(context.getString(app.possessory.R.string.inventory_report), 40f, 80f, 15f)
            text("${java.time.LocalDate.now()}  ·  $pageNum", 40f, 104f, 10f)
            text(context.getString(app.possessory.R.string.report_disclaimer), 40f, 805f, 8f)
        }
        fun lines(value: String, size: Float = 10f) {
            paint.textSize = size
            var remaining = value.replace('\n', ' ')
            if (remaining.isEmpty()) return
            while (remaining.isNotEmpty()) {
                if (y > 770f) newPage()
                val end = paint.breakText(remaining, true, 515f, null).coerceAtLeast(1)
                text(remaining.take(end), 40f, y, size)
                remaining = remaining.drop(end)
                y += 16f
            }
        }
        try {
            newPage()
            items.forEach { item ->
                if (y > 660f) newPage()
                lines(item.name, 15f)
                lines(rooms.find { it.id == item.roomId }?.name ?: "")
                if (prices)
                    lines(
                        "${context.getString(app.possessory.R.string.purchase_price)}: ${item.priceMinor?.let { InventoryRules.money(it,item.currency) } ?: "—"}"
                    )
                if (item.purchaseDate.isNotBlank())
                    lines(
                        "${context.getString(app.possessory.R.string.purchase_date)}: ${item.purchaseDate}"
                    )
                if (serials && item.serial.isNotBlank())
                    lines("${context.getString(app.possessory.R.string.serial)}: ${item.serial}")
                if (item.warrantyEnd.isNotBlank())
                    lines(
                        "${context.getString(app.possessory.R.string.warranty_end)}: ${item.warrantyEnd}"
                    )
                y += 18f
            }
            document.finishPage(page!!)
            page = null
            document.writeTo(output)
        } finally {
            document.close()
        }
    }
}

object BackupCrypto {
    // Fire's Conscrypt GCM provider buffers whole messages; use an explicit streaming provider.
    private val streamingProvider by lazy { org.bouncycastle.jce.provider.BouncyCastleProvider() }
    private val magic = "POSSENC1".toByteArray()

    fun encrypting(output: OutputStream, password: CharArray): OutputStream {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val header = magic + salt + iv
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", streamingProvider)
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        output.write(header)
        return CipherOutputStream(output, cipher)
    }

    /** Caller must discard the output if authentication fails; never import before completion. */
    fun decrypt(
        input: InputStream,
        output: OutputStream,
        password: CharArray,
        max: Long = 100L * 1024 * 1024,
    ) {
        val header = ByteArray(36)
        DataInputStream(input).readFully(header)
        require(header.take(8).toByteArray().contentEquals(magic)) { "invalid_backup" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", streamingProvider)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(password, header.copyOfRange(8, 24)),
            GCMParameterSpec(128, header.copyOfRange(24, 36)),
        )
        cipher.updateAAD(header)
        val buffer = ByteArray(8192)
        var total = 0L
        // Explicit update/doFinal avoids providers that silently suppress an invalid GCM tag.
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= max + 16) { "file_too_large" }
            cipher.update(buffer, 0, count)?.let { output.write(it) }
        }
        output.write(cipher.doFinal())
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size >= 12) { "password_length" }
        val spec = PBEKeySpec(password, salt, 600000, 256)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES",
            )
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(bytes: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val header = magic + salt + iv
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(bytes)
    }

    fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
        require(bytes.size >= 52 && bytes.take(8).toByteArray().contentEquals(magic)) {
            "invalid_backup"
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(password, bytes.copyOfRange(8, 24)),
            GCMParameterSpec(128, bytes.copyOfRange(24, 36)),
        )
        cipher.updateAAD(bytes.copyOfRange(0, 36))
        return cipher.doFinal(bytes, 36, bytes.size - 36)
    }
}
