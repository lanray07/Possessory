package app.possessory

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
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
        assertFalse(
            "Unlock the tablet before interactive device checks",
            app.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked,
        )
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
        cancelExternal()
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
            cancelExternal()
            ui.waitForIdle()
            assertEquals(initial, vm.dao.attachments().first().size)
        }
    }

    private fun waitForExternalPackage() {
        ui.waitUntil(10000) { !targetResumed() }
        // onPause precedes the external window gaining focus. Do not send Back to the old window.
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(750)
    }

    private fun targetResumed(): Boolean {
        var resumed = false
        instrumentation.runOnMainSync {
            resumed =
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .any { it is MainActivity }
        }
        return resumed
    }

    private fun cancelExternal() {
        // SAF may initially expose a roots drawer; Back first closes that nested UI.
        repeat(3) {
            if (!targetResumed()) {
                pressBack()
                val until = android.os.SystemClock.elapsedRealtime() + 1200
                while (!targetResumed() && android.os.SystemClock.elapsedRealtime() < until) android
                    .os
                    .SystemClock
                    .sleep(50)
            }
        }
        ui.waitUntil(10000) { targetResumed() }
    }

    private fun pressBack() {
        assertTrue("External Back event was rejected", UiDevice.getInstance(instrumentation).pressBack())
    }
}
