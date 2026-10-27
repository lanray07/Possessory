package app.possessory

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.possessory.data.*
import java.io.File
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], application = TestApp::class)
class LocalizedRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "es-rES-w1280dp-h800dp-land-mdpi")
    fun spanishRoomScreenUsesBundledResourcesAndRetainsUserNames() {
        render("Habitación por habitación.", "spanish")
    }

    @Test
    @Config(qualifiers = "ar-ldrtl-w1280dp-h800dp-land-mdpi")
    fun arabicRoomScreenRendersOffline() {
        render("غرفة تلو الأخرى.", "arabic")
    }

    private fun render(heading: String, locale: String) {
        val rooms = listOf(RoomRecord("room", "home", "Alice’s studio", "Home Office"))
        compose.setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { RoomScreen(rooms, emptyList(), {}, {}) }
            }
        }
        compose.onNodeWithText(heading).assertIsDisplayed()
        compose.onNodeWithText("Alice’s studio").assertIsDisplayed()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output =
                File("../artifacts/renders/localized-$locale-tablet.png").apply {
                    parentFile!!.mkdirs()
                }
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
