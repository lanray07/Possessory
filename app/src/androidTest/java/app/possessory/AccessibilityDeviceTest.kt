package app.possessory

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Exercises keyboard focus and accessible actions; does not claim a human screen-reader review. */
@RunWith(AndroidJUnit4::class)
class AccessibilityDeviceTest {
  @get:Rule val ui = createAndroidComposeRule<MainActivity>()
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val app
    get() = instrumentation.targetContext

  private val device
    get() = UiDevice.getInstance(instrumentation)

  private var language: String? = null

  @Before
  fun isolatedReviewOnly() {
    org.junit.Assume.assumeTrue(BuildConfig.REVIEW_MODE)
    assertEquals("app.possessory.review", app.packageName)
    assertFalse(app.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)
    language = AppLanguage.selected(app)
    instrumentation.runOnMainSync { AppLanguage.select(app, "en-GB") }
    ui.activityRule.scenario.recreate()
    ui.waitUntil(10000) { ui.onAllNodesWithText("Willow House").fetchSemanticsNodes().isNotEmpty() }
  }

  @After
  fun restoreLanguage() {
    language?.let { value -> instrumentation.runOnMainSync { AppLanguage.select(app, value) } }
  }

  @Test
  fun compactAddActionIsLabelledAndOpensEditorWithoutSaving() {
    ui.runOnUiThread {
      ui.activity.setContent { Box(Modifier.width(400.dp).fillMaxHeight()) { Possessory() } }
    }
    ui.waitForIdle()
    ui.onNodeWithContentDescription("Add item")
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
    ui.onNodeWithText("Item name").assertIsDisplayed()
    ui.onNodeWithText("Cancel").performClick()
    ui.waitUntil(5000) { ui.onAllNodesWithText("Item name").fetchSemanticsNodes().isEmpty() }
  }

  private fun tabTo(label: String) {
    repeat(32) {
      if (ui.onAllNodes(hasText(label) and isFocused()).fetchSemanticsNodes().isNotEmpty()) return
      assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_TAB))
      ui.waitForIdle()
    }
    fail("Keyboard Tab could not reach $label")
  }

  @Test
  fun keyboardTabAndEnterNavigateInventoryAndSettings() {
    tabTo("Inventory")
    assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_ENTER))
    ui.waitUntil(5000) {
      ui.onAllNodesWithText(app.getString(R.string.your_belongings))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    ui.onNodeWithText(app.getString(R.string.your_belongings)).assertIsDisplayed()
    tabTo("Settings")
    assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_ENTER))
    ui.waitUntil(5000) {
      ui.onAllNodesWithText(app.getString(R.string.settings_subtitle))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    ui.onNodeWithText(app.getString(R.string.settings_subtitle)).assertIsDisplayed()
  }
}
