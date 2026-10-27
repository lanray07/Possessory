package app.possessory.services

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.room.withTransaction
import app.possessory.InventoryState
import app.possessory.PossessoryApp
import app.possessory.data.*
import app.possessory.domain.InventoryRules
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
  val schema: Int = 1,
  val createdAt: Long = System.currentTimeMillis(),
  val households: List<Household>,
  val rooms: List<RoomRecord>,
  val items: List<InventoryItem>,
  val attachments: List<Attachment>,
  val maintenance: List<Maintenance>,
  val hashes: Map<String, String>,
)

object Files {
  private const val LIMIT = 100L * 1024 * 1024
  val gson = Gson()

  fun readBounded(input: InputStream, max: Long = LIMIT): ByteArray =
    ByteArrayOutputStream().also { copyBounded(input, it, max) }.toByteArray()

  fun hash(bytes: ByteArray) =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

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

  fun backup(context: Context, s: InventoryState, output: OutputStream) {
    val files =
      s.attachments.associate { it.file to File(context.filesDir, "attachments/${it.file}") }
    require(files.values.all { it.isFile }) { "missing_attachment" }
    require(files.values.sumOf { it.length() } <= LIMIT - 1024 * 1024) { "backup_too_large" }
    val snapshot =
      Snapshot(
        households = s.households,
        rooms = s.rooms,
        items = s.items,
        attachments = s.attachments,
        maintenance = s.maintenance,
        hashes = files.mapValues { hash(it.value.readBytes()) },
      )
    ZipOutputStream(output).use { zip ->
      zip.putNextEntry(ZipEntry("manifest.json"))
      zip.write(gson.toJson(snapshot).toByteArray(Charsets.UTF_8))
      zip.closeEntry()
      files.forEach { (name, file) ->
        zip.putNextEntry(ZipEntry("attachments/$name"))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
      }
    }
  }

  data class Preview(val snapshot: Snapshot, val files: Map<String, ByteArray>)

  fun preview(input: InputStream): Preview {
    val entries = mutableMapOf<String, ByteArray>()
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
        val output = ByteArrayOutputStream()
        copyBounded(zip, output, LIMIT - total)
        val bytes = output.toByteArray()
        total += bytes.size
        entries[entry.name] = bytes
      }
    }
    val manifest = entries.remove("manifest.json") ?: error("invalid_backup")
    val s = gson.fromJson(String(manifest, Charsets.UTF_8), Snapshot::class.java)
    require(s.schema == 1 && s.items.size <= 100000) { "unsupported_schema" }
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
          item.reminderDays in 0..365
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
    require(s.attachments.map { it.file }.distinct().size == s.attachments.size)
    require(entries.keys == s.attachments.map { "attachments/${it.file}" }.toSet())
    require(s.hashes.keys == s.attachments.map { it.file }.toSet())
    s.attachments.forEach { a ->
      require(
        s.items.any { it.id == a.itemId } &&
          a.mime in listOf("image/jpeg", "image/png", "image/webp", "application/pdf")
      )
      require(
        s.hashes[a.file] == hash(entries["attachments/${a.file}"] ?: error("missing_attachment"))
      ) {
        "integrity_failed"
      }
    }
    return Preview(s, entries)
  }

  suspend fun restore(app: PossessoryApp, preview: Preview) {
    val s = preview.snapshot
    val dao = app.db.inventory()
    // Merge by stable IDs; existing records win. Remap imported file names so no existing file is
    // overwritten.
    val current = dao.attachments().first()
    val existingItems = dao.items().first().map { it.id }.toSet()
    val files = mutableListOf<File>()
    try {
      val imported =
        s.attachments
          .filter { a -> current.none { it.id == a.id } && a.itemId !in existingItems }
          .map { a ->
            val name = UUID.randomUUID().toString()
            val file = File(app.filesDir, "attachments/$name").apply { parentFile!!.mkdirs() }
            files.add(file)
            file.writeBytes(preview.files.getValue("attachments/${a.file}"))
            a.copy(file = name)
          }
      app.db.withTransaction {
        val homes = dao.households().first().map { it.id }.toSet()
        val rooms = dao.rooms().first().map { it.id }.toSet()
        val maintenance = dao.maintenance().first().map { it.id }.toSet()
        s.households.filter { it.id !in homes }.forEach { dao.put(it) }
        s.rooms.filter { it.id !in rooms }.forEach { dao.put(it) }
        s.items.filter { it.id !in existingItems }.forEach { dao.put(it) }
        imported.forEach { dao.put(it) }
        s.maintenance
          .filter { it.id !in maintenance && it.itemId !in existingItems }
          .forEach { dao.put(it) }
      }
    } catch (e: Exception) {
      files.forEach { it.delete() }
      throw e
    }
  }

  fun csv(
    items: List<InventoryItem>,
    rooms: List<RoomRecord>,
    serials: Boolean,
    prices: Boolean,
  ): String =
    "\uFEFF" +
      (listOf(
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
        ) +
          items.map { i ->
            listOf(
              i.name,
              rooms.find { it.id == i.roomId }?.name ?: "",
              i.category,
              i.purchaseDate,
              if (prices) InventoryRules.editableAmount(i.priceMinor, i.currency) else "",
              i.currency,
              if (serials) i.serial else "",
              i.retailer,
              i.warrantyEnd,
            )
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
          lines("${context.getString(app.possessory.R.string.purchase_date)}: ${item.purchaseDate}")
        if (serials && item.serial.isNotBlank())
          lines("${context.getString(app.possessory.R.string.serial)}: ${item.serial}")
        if (item.warrantyEnd.isNotBlank())
          lines("${context.getString(app.possessory.R.string.warranty_end)}: ${item.warrantyEnd}")
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
  private val magic = "POSSENC1".toByteArray()

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
