package app.possessory

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import app.possessory.data.*
import app.possessory.services.*
import java.io.File
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Only the isolated review package is allowed to create these disposable QA fixtures. */
@RunWith(AndroidJUnit4::class)
class PlatformReadinessTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app
        get() = instrumentation.targetContext.applicationContext as PossessoryApp

    @Before
    fun requireReview() {
        org.junit.Assume.assumeTrue(BuildConfig.REVIEW_MODE)
        assertEquals("app.possessory.review", app.packageName)
    }

    @Test
    fun recordActualPlatformCapabilities() {
        val ready = CountDownLatch(1)
        var status = TextToSpeech.ERROR
        lateinit var tts: TextToSpeech
        instrumentation.runOnMainSync {
            tts =
                TextToSpeech(app) { result ->
                    status = result
                    ready.countDown()
                }
        }
        val initialized = ready.await(20, TimeUnit.SECONDS) && status == TextToSpeech.SUCCESS
        val report =
            linkedMapOf<String, Any>(
                "model" to android.os.Build.MODEL,
                "android_api" to android.os.Build.VERSION.SDK_INT,
                "device_credential_configured" to
                    app.getSystemService(KeyguardManager::class.java).isDeviceSecure,
                "speech_recognition_available" to SpeechRecognizer.isRecognitionAvailable(app),
                "camera_handler_available" to
                    (Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                        .resolveActivity(app.packageManager) != null),
                "document_picker_available" to
                    (Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("application/pdf")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .resolveActivity(app.packageManager) != null),
                "document_export_available" to
                    (Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .setType("application/pdf")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .resolveActivity(app.packageManager) != null),
                "notifications_enabled" to
                    app.getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
                "tts_initialized" to initialized,
                "human_accessibility_or_audio_signoff" to false,
            )
        if (initialized)
            report["tts_language_availability"] =
                listOf("en-GB", "de-DE", "ar", "ja-JP", "hi-IN").associateWith {
                    tts.isLanguageAvailable(Locale.forLanguageTag(it))
                }
        tts.shutdown()
        File(app.getExternalFilesDir(null), "platform-readiness.json")
            .writeText(Files.gson.toJson(report))
        assertEquals(true, report["document_picker_available"])
        assertEquals(true, report["document_export_available"])
    }

    @Test
    fun nativeContentProviderPreservesImportedImageAndPdf() {
        val directory = File(app.filesDir, "attachments").apply { mkdirs() }
        val image = File(directory, "qa-provider-fixture.png")
        val pdf = File(directory, "qa-provider-fixture.pdf")
        val imported = mutableListOf<File>()
        try {
            val bitmap = Bitmap.createBitmap(300, 180, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            Canvas(bitmap)
                .drawText(
                    "QA fixture GBP 49.99",
                    12f,
                    80f,
                    Paint().apply {
                        color = Color.BLACK
                        textSize = 20f
                    },
                )
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val document = PdfDocument()
            try {
                val page = document.startPage(PdfDocument.PageInfo.Builder(300, 180, 1).create())
                page.canvas.drawText(
                    "QA receipt GBP 49.99",
                    12f,
                    80f,
                    Paint().apply { textSize = 20f },
                )
                document.finishPage(page)
                pdf.outputStream().use { document.writeTo(it) }
            } finally {
                document.close()
            }
            for ((source, mime) in listOf(image to "image/png", pdf to "application/pdf")) {
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", source)
                val attachment = Files.importAttachment(app, uri, "qa-provider-item", "Receipt")
                val copy = File(directory, attachment.file).also { imported += it }
                assertEquals(mime, attachment.mime)
                assertEquals(Files.hash(source), Files.hash(copy))
                assertTrue(source.exists())
                ReceiptOcr.bitmap(copy, mime, 0).recycle()
            }
        } finally {
            imported.forEach { it.delete() }
            image.delete()
            pdf.delete()
        }
    }

    @Test
    fun dueReminderRunsThroughWorkManagerAndOptOutCancels() = runBlocking {
        val dao = app.db.inventory()
        val prefs = app.settings.data.first()
        val manager = app.getSystemService(NotificationManager::class.java)
        val workManager = WorkManager.getInstance(app)
        val item =
            InventoryItem(
                "qa-notification-item",
                "QA reminder fixture",
                "qa-notification-room",
                warrantyEnd = LocalDate.now().toString(),
                reminderOffsets = "0",
            )
        var work: OneTimeWorkRequest? = null
        try {
            dao.put(Household("qa-notification-home", "QA notification fixture"))
            dao.put(RoomRecord("qa-notification-room", "qa-notification-home", "QA room"))
            dao.put(item)
            app.settings.edit {
                it[stringPreferencesKey("reminders")] = "on"
                it[stringPreferencesKey("last_reminder_check")] =
                    LocalDate.now().minusDays(1).toString()
            }
            val request = OneTimeWorkRequestBuilder<ReminderWorker>().build().also { work = it }
            workManager.enqueue(request).result.get(10, TimeUnit.SECONDS)
            withTimeout(30000) {
                while (
                    workManager.getWorkInfoById(request.id).get()?.state != WorkInfo.State.SUCCEEDED
                ) delay(100)
            }
            assertTrue(
                manager.activeNotifications.any {
                    it.id == 41 && it.notification.contentIntent != null
                }
            )
            assertEquals(
                LocalDate.now().toString(),
                app.settings.data.first()[stringPreferencesKey("last_reminder_check")],
            )
            val vm = InventoryViewModel(app)
            instrumentation.runOnMainSync { vm.setting("reminders", "off") }
            withTimeout(10000) {
                while (manager.activeNotifications.any { it.id == 41 }) delay(100)
            }
        } finally {
            work?.let { workManager.cancelWorkById(it.id).result.get(10, TimeUnit.SECONDS) }
            manager.cancel(41)
            dao.delete(item)
            app.db.openHelper.writableDatabase.execSQL(
                "DELETE FROM rooms WHERE id = 'qa-notification-room'"
            )
            app.db.openHelper.writableDatabase.execSQL(
                "DELETE FROM households WHERE id = 'qa-notification-home'"
            )
            app.settings.edit { current ->
                for (name in listOf("reminders", "last_reminder_check")) {
                    val key = stringPreferencesKey(name)
                    prefs[key]?.let { current[key] = it } ?: current.remove(key)
                }
            }
        }
    }
}
