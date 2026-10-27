package app.possessory

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import app.possessory.data.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w1280dp-h800dp-land-mdpi", application = TestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderedUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val home = Household("home", "Willow House")
    private val rooms =
        listOf(
            RoomRecord("living", "home", "Living Room", "Living Room"),
            RoomRecord("kitchen", "home", "Kitchen", "Kitchen"),
            RoomRecord("office", "home", "Home Office", "Home Office"),
        )
    private val items =
        listOf(
            InventoryItem(
                "coffee",
                "Espresso machine",
                "kitchen",
                "Appliances",
                brand = "Sage",
                priceMinor = 44900,
                warrantyEnd = java.time.LocalDate.now().plusDays(14).toString(),
            ),
            InventoryItem("lamp", "Reading lamp", "living", "Furniture", priceMinor = 8900),
            InventoryItem(
                "camera",
                "Mirrorless camera",
                "office",
                "Electronics",
                priceMinor = 129900,
            ),
        )

    @Test
    fun dashboardRendersActualComponentsAndQuickAddWorks() {
        var added = false
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Navy, background = Ivory)) {
                Surface(Modifier.fillMaxSize(), color = Ivory) {
                    Dashboard(
                        home,
                        rooms,
                        items,
                        InventoryState(listOf(home), rooms, items),
                        true,
                        { added = true },
                        {},
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        compose.onNodeWithText("Everything, in its place.").assertIsDisplayed()
        compose.onNodeWithText("Add item").performClick()
        Assert.assertTrue(added)
        render("dashboard-tablet.png")
    }

    @Test
    fun inventorySearchActuallyFiltersResults() {
        compose.setContent {
            var query by remember { mutableStateOf("") }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    InventoryScreen(
                        items,
                        rooms,
                        emptyList(),
                        emptyList(),
                        query,
                        { query = it },
                        "",
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("espresso")
        compose.onNodeWithText("Espresso machine").assertIsDisplayed()
        compose.onNodeWithText("Mirrorless camera").assertDoesNotExist()
        render("inventory-search-tablet.png")
    }

    @Test
    fun roomCardsUseStoredCounts() {
        compose.setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize()) { RoomScreen(rooms, items, {}, {}) } }
        }
        compose.onNodeWithText("Room by room.").assertIsDisplayed()
        compose.onNodeWithText("Kitchen").assertIsDisplayed()
        render("rooms-tablet.png")
    }

    @Test
    fun wholeAppReadsDatabaseAndNavigatesAcrossFeatures() {
        val app = ApplicationProvider.getApplicationContext<TestApp>()
        runBlocking {
            app.db.withTransaction {
                val dao = app.db.inventory()
                dao.clearMaintenance()
                dao.clearAttachments()
                dao.clearItems()
                dao.clearRooms()
                dao.clearHouseholds()
                dao.put(home)
                rooms.forEach { dao.put(it) }
                items.forEach { dao.put(it) }
            }
        }
        val vm = InventoryViewModel(app)
        compose.setContent { Possessory(vm) }
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Willow House").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Everything, in its place.").assertIsDisplayed()
        render("app-overview-tablet.png")
        compose.onNodeWithText("Warranties").performClick()
        compose.onNodeWithText("A little peace of mind.").assertIsDisplayed()
        render("app-warranties-tablet.png")
        compose.onNodeWithText("Reports").performClick()
        compose.onNodeWithText("Your home, on record.").assertIsDisplayed()
        render("app-reports-tablet.png")
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Make yourself at home.").assertIsDisplayed()
        render("app-settings-tablet.png")
    }

    private fun render(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val dir = File("../artifacts/renders").apply { mkdirs() }
            File(dir, name).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
