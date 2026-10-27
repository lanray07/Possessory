package app.possessory

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in, read-only capture of the previously installed fictional demo inventory. */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureProductionDemoWithoutEditingRecords() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        org.junit.Assume.assumeTrue(
            InstrumentationRegistry.getArguments().getString("captureExistingDemo") == "true"
        )
        org.junit.Assume.assumeFalse(BuildConfig.REVIEW_MODE || BuildConfig.DEBUG)
        val app = instrumentation.targetContext.applicationContext as PossessoryApp
        assertEquals("app.possessory", app.packageName)
        assertFalse("Unlock the tablet before screenshot capture", app.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)
        assertFalse(app.getSharedPreferences("app-lock", 0).getBoolean("enabled", false))
        val dao = app.db.inventory()
        val before = dao.items().first()
        assertEquals(6, before.size)
        assertTrue(dao.households().first().any { it.name == "Willow House" })
        val destination =
            File(app.getExternalFilesDir(null), "store-captures-${BuildConfig.VERSION_NAME}")
                .apply { mkdirs() }
        val pages =
            listOf(
                R.string.overview to R.string.everything_in_place,
                R.string.inventory to R.string.your_belongings,
                R.string.rooms to R.string.room_by_room,
                R.string.warranties to R.string.warranty_title,
                R.string.maintenance to R.string.maintenance_title,
                R.string.reports to R.string.reports_title,
                R.string.settings to R.string.settings_subtitle,
            )
        pages.forEachIndexed { index, (navigation, heading) ->
            ui.onAllNodesWithText(app.getString(navigation), useUnmergedTree = true)
                .onFirst()
                .performScrollTo()
                .performClick()
            ui.waitUntil(10000) {
                ui.onAllNodesWithText(app.getString(heading), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            ui.waitForIdle()
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(250)
            instrumentation.uiAutomation.takeScreenshot().also { image ->
                File(destination, "${index+1}.png").outputStream().use {
                    image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                image.recycle()
            }
        }
        assertEquals(before, dao.items().first())
    }
}
