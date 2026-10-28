package app.possessory

import android.app.KeyguardManager
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import app.possessory.services.Files
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in owner-assisted check. Never reads or stores the device credential. */
@RunWith(AndroidJUnit4::class)
class AppLockDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app
        get() = instrumentation.targetContext.applicationContext as PossessoryApp

    private fun phase(value: String) {
        File(app.getExternalFilesDir(null), "app-lock-check-phase.json")
            .writeText(Files.gson.toJson(mapOf("phase" to value, "package" to app.packageName)))
    }

    private fun resumed(): Boolean {
        var result = false
        instrumentation.runOnMainSync {
            result =
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .any { it is MainActivity }
        }
        return result
    }

    private fun back() {
        assertTrue(
            "Credential-screen Back event was rejected",
            UiDevice.getInstance(instrumentation).pressBack(),
        )
    }

    private fun cancelCredential() {
        // The first Back can dismiss the credential keyboard rather than cancel authentication.
        repeat(3) {
            if (!resumed()) {
                back()
                val until = android.os.SystemClock.elapsedRealtime() + 1200
                while (!resumed() && android.os.SystemClock.elapsedRealtime() < until) android.os
                    .SystemClock
                    .sleep(50)
            }
        }
        ui.waitUntil(10000) { resumed() }
    }

    private fun waitForCredential() {
        ui.waitUntil(10000) { !resumed() }
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(400)
    }

    private fun secure(): Boolean {
        var result = false
        ui.runOnUiThread {
            result =
                ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        }
        return result
    }

    @Test
    fun credentialCancellationBackgroundRecreationAndScreenshotProtection() {
        org.junit.Assume.assumeTrue(
            BuildConfig.REVIEW_MODE &&
                InstrumentationRegistry.getArguments().getString("ownerCredentialChecks") == "true"
        )
        assertEquals("app.possessory.review", app.packageName)
        val keyguard = app.getSystemService(KeyguardManager::class.java)
        assertTrue(keyguard.isDeviceSecure)
        assertFalse(keyguard.isKeyguardLocked)
        val store = app.getSharedPreferences("app-lock", 0)
        org.junit.Assume.assumeFalse(store.getBoolean("enabled", false))
        val vm = InventoryViewModel(app)
        try {
            ui.runOnUiThread { ui.activity.setContent { MaterialTheme { AppLockSetting(vm) } } }
            ui.waitForIdle()
            ui.onNode(isToggleable()).performClick()
            waitForCredential()
            cancelCredential()
            assertFalse(store.getBoolean("enabled", false))
            assertFalse(secure())
            ui.onNode(isToggleable()).performClick()
            waitForCredential()
            phase("owner-confirm-enable")
            ui.waitUntil(240000) { store.getBoolean("enabled", false) && resumed() }
            ui.waitForIdle()
            ui.onNodeWithText(app.getString(R.string.app_locked)).assertIsDisplayed()
            assertTrue(secure())
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(300)
            val screenshot =
                runCatching { instrumentation.uiAutomation.takeScreenshot() }.getOrNull()
            val protected =
                screenshot == null ||
                    (screenshot.getPixel(screenshot.width / 2, screenshot.height / 2) and
                        0x00ffffff) == 0
            screenshot?.recycle()
            assertTrue("Secure app window leaked into a screenshot", protected)
            ui.onNodeWithText(app.getString(R.string.unlock)).performClick()
            waitForCredential()
            cancelCredential()
            ui.onNodeWithText(app.getString(R.string.app_locked)).assertIsDisplayed()
            ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            ui.onNodeWithText(app.getString(R.string.app_locked)).assertIsDisplayed()
            ui.activityRule.scenario.recreate()
            ui.onNodeWithText(app.getString(R.string.app_locked)).assertIsDisplayed()
            assertTrue(secure())
            ui.onNodeWithText(app.getString(R.string.unlock)).performClick()
            waitForCredential()
            phase("owner-confirm-unlock")
            ui.waitUntil(240000) { resumed() }
            ui.waitUntil(10000) {
                ui.onAllNodesWithText(app.getString(R.string.app_locked))
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
            assertTrue(secure())
            ui.runOnUiThread { ui.activity.setContent { MaterialTheme { AppLockSetting(vm) } } }
            ui.waitForIdle()
            ui.onNode(isToggleable()).performClick()
            ui.waitUntil(10000) { !store.getBoolean("enabled", false) && resumed() }
            ui.waitForIdle()
            assertFalse(secure())
            phase("complete")
        } finally {
            // Restore the original setting only in the isolated QA package; never alter the device
            // PIN.
            store.edit().putBoolean("enabled", false).commit()
            runCatching { if (!resumed()) back() }
        }
    }
}
