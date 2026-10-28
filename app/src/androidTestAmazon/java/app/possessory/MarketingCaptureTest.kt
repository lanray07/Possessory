package app.possessory

import android.graphics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.possessory.data.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Actual navigation in the isolated, verified App Tester app; never touches production records. */
@RunWith(AndroidJUnit4::class)
class MarketingCaptureTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun sixCoreScreens() = runBlocking {
        Assume.assumeTrue(BuildConfig.APPLICATION_ID.endsWith(".iapqa"))
        assertTrue(BuildConfig.DEBUG)
        assertFalse(BuildConfig.REVIEW_MODE)
        val inst = InstrumentationRegistry.getInstrumentation()
        val app = inst.targetContext.applicationContext as PossessoryApp
        assertFalse(app.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)
        withTimeout(60000) { app.billing.state.first { it.premium && !it.busy } }
        val dao = app.db.inventory()
        assertTrue("Only this test's disposable fixtures may exist",
            dao.items().first().all { it.id.startsWith("marketing-") })
        val today = LocalDate.now()
        val home = Household("marketing-home", "Willow House")
        val rooms = listOf(RoomRecord("marketing-living", home.id, "Living Room", "Living Room"),
            RoomRecord("marketing-kitchen", home.id, "Kitchen", "Kitchen"),
            RoomRecord("marketing-study", home.id, "Study", "Office"))
        val names = listOf("Reading lamp", "Linen sofa", "Coffee machine", "Stand mixer", "Laptop", "Desk chair")
        val items = names.mapIndexed { i, name -> InventoryItem("marketing-$i", name, rooms[i/2].id,
            priceMinor = listOf(4999L,79900L,18900L,24900L,89900L,17900L)[i],
            purchaseDate = today.minusMonths(5).toString(),
            warrantyEnd = today.plusDays(if (i == 0) 14 else 160+i.toLong()).toString(),
            retailer = "Willow Shop") }
        dao.put(home); rooms.forEach { dao.put(it) }; items.forEach { dao.put(it) }
        dao.put(Maintenance("marketing-maintenance", items[2].id, "Cleaning", today.minusDays(30).toString(), today.plusDays(7).toString()))
        val receipt = File(app.filesDir, "attachments/marketing-receipt.png").apply { parentFile!!.mkdirs() }
        val bitmap = Bitmap.createBitmap(1200, 520, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 56f }
        Canvas(bitmap).apply {
            drawText("WILLOW SHOP - DEMO RECEIPT", 50f, 90f, paint)
            drawText("Reading lamp", 50f, 190f, paint)
            drawText("TOTAL GBP 49.99", 50f, 290f, paint)
            drawText(today.minusMonths(5).toString(), 50f, 390f, paint)
        }
        receipt.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        dao.put(Attachment("marketing-receipt", items[0].id, "Demo receipt", "image/png", receipt.name))
        val output = File(app.getExternalFilesDir(null), "marketing-captures").apply { mkdirs() }
        fun capture(name: String) {
            ui.waitForIdle(); inst.waitForIdleSync(); android.os.SystemClock.sleep(700)
            inst.uiAutomation.takeScreenshot().also { image ->
                File(output, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                image.recycle()
            }
        }
        fun page(nav: Int, heading: Int) {
            ui.onAllNodesWithText(app.getString(nav), useUnmergedTree = true).onFirst().performScrollTo().performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText(app.getString(heading)).fetchSemanticsNodes().isNotEmpty() }
        }
        ui.waitUntil(20000) { runCatching { ui.onAllNodesWithText("Willow House", substring = true).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        page(R.string.overview, R.string.everything_in_place); capture("hero")
        page(R.string.rooms, R.string.room_by_room); capture("rooms")
        page(R.string.warranties, R.string.warranty_title); capture("warranties")
        page(R.string.reports, R.string.reports_title)
        ui.onAllNodesWithText(app.getString(R.string.report_preview)).onFirst().performScrollTo().performClick()
        ui.waitUntil(15000) { File(app.cacheDir, "report-preview.pdf").length() > 100 }
        ui.waitUntil(20000) { ui.onAllNodesWithContentDescription(app.getString(R.string.report_preview)).fetchSemanticsNodes().isNotEmpty() }
        capture("reports")
        ui.onNodeWithText(app.getString(R.string.close)).performClick()
        ui.onNodeWithContentDescription(app.getString(R.string.voice)).performClick()
        ui.onNodeWithText(app.getString(R.string.voice_unavailable)).assertExists()
        capture("voice")
        androidx.test.uiautomator.UiDevice.getInstance(inst).pressBack()
        page(R.string.inventory, R.string.your_belongings)
        ui.onNode(hasSetTextAction()).performTextReplacement("Reading lamp")
        ui.runOnIdle {
            ui.activity.currentFocus?.let { focus ->
                ui.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                    .hideSoftInputFromWindow(focus.windowToken, 0)
                focus.clearFocus()
            }
        }
        ui.waitForIdle()
        ui.onAllNodes(hasScrollToNodeAction()).onLast()
            .performScrollToNode(hasText("Reading lamp") and !hasSetTextAction())
        ui.onNode(hasText("Reading lamp") and !hasSetTextAction()).performScrollTo().performClick()
        ui.onAllNodes(hasScrollToNodeAction()).onLast()
            .performScrollToNode(hasText(app.getString(R.string.warranty_end)))
        capture("reminders")
        // OCR and offline screenshots are completed through native UI capture after this test.
        // The receipt loader must also be visually checked outside the Compose test clock.
    }
}
