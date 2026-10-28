package app.possessory

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import app.possessory.data.*
import app.possessory.services.Files
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Uses synthetic/disposable records only in .review; never touches production inventory. */
@RunWith(AndroidJUnit4::class)
class PositiveAttachmentDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app
        get() = instrumentation.targetContext.applicationContext as PossessoryApp

    private val token = "qa-attachment-" + UUID.randomUUID()
    private lateinit var item: InventoryItem
    private lateinit var vm: InventoryViewModel

    private fun phase(value: String, extra: Map<String, Any> = emptyMap()) {
        File(app.getExternalFilesDir(null), "attachment-check-phase.json")
            .writeText(Files.gson.toJson(mapOf("phase" to value) + extra))
    }

    @Before
    fun prepare() = runBlocking {
        org.junit.Assume.assumeTrue(
            BuildConfig.REVIEW_MODE &&
                InstrumentationRegistry.getArguments().getString("attachmentChecks") == "true"
        )
        assertEquals("app.possessory.review", app.packageName)
        assertFalse(app.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)
        vm = InventoryViewModel(app)
        vm.dao.put(Household(token, "QA disposable household"))
        vm.dao.put(RoomRecord(token, token, "QA disposable room"))
        item = InventoryItem(token, "QA attachment check", token)
        vm.dao.put(item)
        phase("starting")
        ui.runOnUiThread {
            ui.activity.setContent {
                val state by vm.state.collectAsState()
                MaterialTheme { DetailScreen(item, state, vm, {}, {}, {}) }
            }
        }
        ui.waitForIdle()
    }

    @After
    fun cleanup() = runBlocking {
        if (::vm.isInitialized) {
            vm.dao
                .attachments()
                .first()
                .filter { it.itemId == token }
                .forEach { File(app.filesDir, "attachments/${it.file}").delete() }
            vm.dao.delete(item)
            app.db.openHelper.writableDatabase.execSQL(
                "DELETE FROM rooms WHERE id = ?",
                arrayOf(token),
            )
            app.db.openHelper.writableDatabase.execSQL(
                "DELETE FROM households WHERE id = ?",
                arrayOf(token),
            )
        }
    }

    @Test
    fun selectSyntheticReceiptThroughSystemPicker() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val filename = requireNotNull(args.getString("qaImageFilename"))
        val expected = requireNotNull(args.getString("qaImageSha256"))
        ui.onNodeWithText(app.getString(R.string.add_receipt)).performScrollTo().performClick()
        phase("picker-open", mapOf("filename" to filename))
        val device = UiDevice.getInstance(instrumentation)
        val match =
            device.wait(Until.findObject(By.textContains(filename.substringBeforeLast('.'))), 6000)
        match?.click()
        if (vm.dao.attachments().first().none { it.itemId == token })
            phase("owner-select-fixture", mapOf("filename" to filename))
        val attachment =
            withTimeout(240000) {
                vm.dao
                    .attachments()
                    .first { list -> list.any { it.itemId == token } }
                    .first { it.itemId == token }
            }
        val file = File(app.filesDir, "attachments/${attachment.file}")
        assertEquals("image/png", attachment.mime)
        assertEquals("Receipt", attachment.kind)
        assertEquals(expected, Files.hash(file))
        ui.waitForIdle()
        phase("picker-complete", mapOf("sha256_verified" to true, "bytes" to file.length()))
    }

    @Test
    fun captureCameraPhotoThroughFileProvider() = runBlocking {
        ui.onNodeWithContentDescription(app.getString(R.string.take_photo))
            .performScrollTo()
            .performClick()
        UiDevice.getInstance(instrumentation).waitForIdle()
        phase("owner-capture-test-target")
        val attachment =
            withTimeout(240000) {
                vm.dao
                    .attachments()
                    .first { list -> list.any { it.itemId == token } }
                    .first { it.itemId == token }
            }
        val file = File(app.filesDir, "attachments/${attachment.file}")
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(file.path, options)
        assertEquals("Photo", attachment.kind)
        assertEquals("image/jpeg", attachment.mime)
        assertTrue(file.length() > 0 && options.outWidth >= 100 && options.outHeight >= 100)
        phase(
            "camera-complete",
            mapOf(
                "bytes" to file.length(),
                "width" to options.outWidth,
                "height" to options.outHeight,
                "mime" to attachment.mime,
            ),
        )
    }
}
