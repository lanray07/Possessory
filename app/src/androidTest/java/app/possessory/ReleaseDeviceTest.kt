package app.possessory

import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.possessory.data.*
import app.possessory.services.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Runs only against the isolated review package; never edits a customer's inventory. */
@RunWith(AndroidJUnit4::class)
class ReleaseDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun requireReviewPackage() {
        org.junit.Assume.assumeTrue(BuildConfig.REVIEW_MODE)
        assertTrue(context.packageName.endsWith(".review"))
    }

    @Test
    fun nativeOcrModelsPdfPaginationAndEncryptedBackup() = runBlocking {
        val bitmap = Bitmap.createBitmap(1200, 500, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 64f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }
        Canvas(bitmap).apply {
            drawText("WILLOW SHOP", 70f, 100f, paint)
            drawText("TOTAL GBP 49.99", 70f, 210f, paint)
            drawText("2026-10-27", 70f, 320f, paint)
        }
        try {
            ReceiptOcr.languages.forEach { language ->
                val text = ReceiptOcr.recognise(context, bitmap, language)
                assertTrue("OCR model $language returned no text", text.isNotBlank())
                if (language == "eng") assertTrue(text.contains("49.99"))
            }
        } finally {
            bitmap.recycle()
        }
        val report = File(context.cacheDir, "pagination-check.pdf")
        val items =
            (1..80).map {
                InventoryItem(
                    "report-$it",
                    "Belonging $it – long name for pagination",
                    "review-room",
                    priceMinor = 4999,
                )
            }
        report.outputStream().use {
            ReportPdf.write(
                context,
                items,
                listOf(RoomRecord("review-room", "review-home", "Living Room")),
                emptyList(),
                false,
                true,
                false,
                false,
                it,
            )
        }
        ParcelFileDescriptor.open(report, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { assertTrue(it.pageCount > 1) }
        }
        val pages = ReceiptOcr.pages(report, "application/pdf")
        assertTrue(pages > 1)
        ReceiptOcr.bitmap(report, "application/pdf", pages - 1).recycle()
        val bytes = "Private backup fixture".toByteArray()
        val encrypted = BackupCrypto.encrypt(bytes, "review-password".toCharArray())
        assertArrayEquals(bytes, BackupCrypto.decrypt(encrypted, "review-password".toCharArray()))
    }

    @Test
    fun largeEncryptedBackupFitsFireMemory() {
        val encrypted = File(context.cacheDir, "large-backup-check.poss")
        val decrypted = File(context.cacheDir, "large-backup-check.zip")
        val attachments =
            (1..4).map {
                Attachment(
                    "performance-$it",
                    "performance-item",
                    "Synthetic binary fixture",
                    "application/pdf",
                    "performance-$it",
                )
            }
        val sourceFiles =
            attachments.map {
                File(context.filesDir, "attachments/${it.file}").apply { parentFile!!.mkdirs() }
            }
        val block = ByteArray(64 * 1024)
        val random = java.util.Random(20261027)
        val started = android.os.SystemClock.elapsedRealtime()
        var preview: Files.Preview? = null
        try {
            sourceFiles.forEach { file ->
                file.outputStream().use { output ->
                    repeat(396) {
                        random.nextBytes(block)
                        output.write(block)
                    }
                }
            }
            val state =
                InventoryState(
                    listOf(Household("performance-home", "Performance fixture")),
                    listOf(RoomRecord("performance-room", "performance-home", "Study")),
                    listOf(InventoryItem("performance-item", "Synthetic data", "performance-room")),
                    attachments,
                )
            encrypted.outputStream().use { file ->
                BackupCrypto.encrypting(file, "review-password".toCharArray()).use {
                    Files.backup(context, state, it)
                }
            }
            encrypted.inputStream().use { input ->
                decrypted.outputStream().use {
                    BackupCrypto.decrypt(input, it, "review-password".toCharArray())
                }
            }
            val checked = decrypted.inputStream().use { Files.preview(it, context.cacheDir) }
            preview = checked
            assertEquals(4, checked.stagedFiles.size)
            assertTrue(checked.files.isEmpty())
            attachments.forEach {
                assertEquals(
                    Files.hash(File(context.filesDir, "attachments/${it.file}")),
                    Files.hash(checked.stagedFiles.getValue("attachments/${it.file}")),
                )
            }
            File(context.getExternalFilesDir(null), "large-backup-check.json")
                .writeText(
                    "{\"attachment_mib\":99,\"round_trip_passed\":true,\"private_staging_passed\":true,\"elapsed_ms\":" +
                        (android.os.SystemClock.elapsedRealtime() - started) +
                        "}"
                )
        } finally {
            preview?.close()
            sourceFiles.forEach { it.delete() }
            encrypted.delete()
            decrypted.delete()
        }
    }

    @Test
    fun captureAllDraftLocalesOnDevice() = runBlocking {
        val app = context.applicationContext as PossessoryApp
        val dao = app.db.inventory()
        dao.put(Household("review-home", "Willow House", "GBP"))
        dao.put(RoomRecord("review-room", "review-home", "Living Room"))
        dao.put(
            InventoryItem(
                "review-camera",
                "Camera",
                "review-room",
                priceMinor = 49999,
                purchaseDate = "2026-10-01",
                warrantyEnd = "2027-10-01",
            )
        )
        val arguments = InstrumentationRegistry.getArguments()
        val captureSet =
            arguments.getString("captureSet", "standard")!!.also {
                require(it.matches(Regex("[a-z-]+")))
            }
        val selected = arguments.getString("locales")?.split(',')
        val root =
            File(context.getExternalFilesDir(null), "locale-captures-$captureSet").apply {
                mkdirs()
            }
        val destinations =
            listOf(
                R.string.overview,
                R.string.inventory,
                R.string.rooms,
                R.string.warranties,
                R.string.maintenance,
                R.string.reports,
                R.string.settings,
            )
        val headings =
            listOf(
                R.string.everything_in_place,
                R.string.your_belongings,
                R.string.room_by_room,
                R.string.warranty_title,
                R.string.maintenance_title,
                R.string.reports_title,
                R.string.settings_subtitle,
            )
        AppLanguage.targets
            .filter { selected == null || it.first in selected }
            .forEach { (locale, _) ->
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    AppLanguage.select(context, locale)
                }
                ui.activityRule.scenario.recreate()
                ui.waitForIdle()
                val localized = AppLanguage.apply(context)
                ui.waitUntil(10000) {
                    ui.onAllNodesWithText(
                            localized.getString(R.string.inventory),
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                destinations.forEachIndexed { index, id ->
                    ui.onAllNodesWithText(localized.getString(id), useUnmergedTree = true)
                        .onFirst()
                        .performScrollTo()
                        .performClick()
                    ui.waitUntil(10000) {
                        ui.onAllNodesWithText(
                                localized.getString(headings[index]),
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNodes()
                            .isNotEmpty()
                    }
                    ui.waitForIdle()
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    android.os.SystemClock.sleep(
                        200
                    ) // Allow the physical display's pending render to commit.
                    val directory = File(root, locale).apply { mkdirs() }
                    InstrumentationRegistry.getInstrumentation()
                        .uiAutomation
                        .takeScreenshot()
                        .also { image ->
                            File(directory, "${index+1}.png").outputStream().use {
                                image.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                            image.recycle()
                        }
                }
            }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppLanguage.select(context, "")
        }
    }
}
