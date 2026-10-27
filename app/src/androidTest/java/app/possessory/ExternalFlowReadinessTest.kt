package app.possessory

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.possessory.data.*
import app.possessory.domain.VoiceIntent
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalFlowReadinessTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app
        get() = instrumentation.targetContext.applicationContext as PossessoryApp

    @Before
    fun isolatedReviewOnly() {
        org.junit.Assume.assumeTrue(BuildConfig.REVIEW_MODE)
        assertEquals("app.possessory.review", app.packageName)
        assertFalse("Unlock the tablet before interactive device checks", app.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)
    }

    @Test
    fun typedVoiceCommandWorksWithoutMicrophonePermission() {
        val received = AtomicReference<VoiceIntent>()
        ui.runOnUiThread {
            ui.activity.setContent { MaterialTheme { VoiceDialog({}, { received.set(it) }) } }
        }
        ui.waitForIdle()
        ui.onNode(hasSetTextAction())
            .performTextInput(app.getString(R.string.voice_warranty_command))
        ui.onNodeWithText(app.getString(R.string.review_command)).performClick()
        ui.waitUntil(5000) { received.get() != null }
        assertNotEquals(VoiceIntent.Unknown, received.get())
        // No speech permission or microphone capture is invoked by typed commands.
    }

    @Test
    fun documentPickerAndCameraCancellationPreserveAttachments() = runBlocking {
        val vm = InventoryViewModel(app)
        val initial = vm.dao.attachments().first().size
        val item = InventoryItem("qa-external-item", "QA external flow", "qa-external-room")
        ui.runOnUiThread {
            ui.activity.setContent {
                val state by vm.state.collectAsState()
                MaterialTheme { DetailScreen(item, state, vm, {}, {}, {}) }
            }
        }
        ui.waitForIdle()
        ui.onNodeWithText(app.getString(R.string.add_receipt)).performScrollTo().performClick()
        waitForExternalPackage()
        pressBack()
        ui.waitUntil(10000) {
            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() ==
                app.packageName
        }
        ui.waitForIdle()
        assertEquals(initial, vm.dao.attachments().first().size)
        if (
            android.content
                .Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                .resolveActivity(app.packageManager) != null
        ) {
            ui.onNodeWithContentDescription(app.getString(R.string.take_photo))
                .performScrollTo()
                .performClick()
            waitForExternalPackage()
            pressBack()
            ui.waitUntil(10000) {
                instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() ==
                    app.packageName
            }
            ui.waitForIdle()
            assertEquals(initial, vm.dao.attachments().first().size)
        }
    }

    private fun waitForExternalPackage() {
        ui.waitUntil(10000) {
            val name = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()
            name != null && name != app.packageName
        }
    }

    private fun pressBack() {
        instrumentation.uiAutomation.injectInputEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK),
            true,
        )
        instrumentation.uiAutomation.injectInputEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK),
            true,
        )
    }
}
