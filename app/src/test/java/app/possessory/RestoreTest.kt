package app.possessory

import androidx.test.core.app.ApplicationProvider
import app.possessory.data.*
import app.possessory.domain.SavedSearch
import app.possessory.domain.SearchCriteria
import app.possessory.services.*
import java.io.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class TestApp : PossessoryApp() {
    override fun onCreate() {
        /* No background scheduling in local tests. */
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApp::class)
class RestoreTest {
    @Test
    fun roomPhotoAndSavedSearchesSurviveBackupAndMerge() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TestApp>()
        val photo = File(app.filesDir, "attachments/room-photo").apply { parentFile!!.mkdirs() }
        val bitmap =
            android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
        photo.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        val search =
            SavedSearch(
                "Valuables",
                "",
                "",
                "",
                "all",
                "name",
                SearchCriteria(currency = "GBP", minimum = "100"),
            )
        val state =
            InventoryState(
                listOf(Household("photo-home", "Home")),
                listOf(RoomRecord("photo-room", "photo-home", "Study", photoFile = photo.name)),
            )
        val output = ByteArrayOutputStream()
        Files.backup(
            app,
            state,
            output,
            mapOf("saved_searches" to Files.gson.toJson(listOf(search))),
        )
        val preview = Files.preview(ByteArrayInputStream(output.toByteArray()), app.cacheDir)
        assertTrue(preview.files.isEmpty())
        assertTrue(preview.stagedFiles.isNotEmpty())
        Files.restore(app, preview)
        val room = app.db.inventory().rooms().first().single()
        assertNotEquals(photo.name, room.photoFile)
        assertArrayEquals(
            photo.readBytes(),
            File(app.filesDir, "attachments/${room.photoFile}").readBytes(),
        )
        val prefs = app.settings.data.first()
        assertTrue(
            prefs[androidx.datastore.preferences.core.stringPreferencesKey("saved_searches")]!!
                .contains("Valuables")
        )
        preview.close()
    }

    @Test
    fun restorePreservesExistingRecordsAndAddsNewOnesIdempotently() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TestApp>()
        val dao = app.db.inventory()
        dao.put(Household("home", "Current home"))
        dao.put(RoomRecord("room", "home", "Current room"))
        dao.put(InventoryItem(id = "existing", name = "Keep this current name", roomId = "room"))
        val file =
            File(app.filesDir, "attachments/incoming").apply {
                parentFile!!.mkdirs()
                writeText("receipt content")
            }
        val state =
            InventoryState(
                listOf(Household("home", "Older home name")),
                listOf(RoomRecord("room", "home", "Older room name")),
                listOf(
                    InventoryItem(id = "existing", name = "Stale name", roomId = "room"),
                    InventoryItem(id = "new", name = "New belonging", roomId = "room"),
                ),
                listOf(Attachment("attachment", "new", "Receipt", "application/pdf", "incoming")),
            )
        val bytes = ByteArrayOutputStream()
        Files.backup(app, state, bytes)
        val preview = Files.preview(ByteArrayInputStream(bytes.toByteArray()))
        Files.restore(app, preview)
        Files.restore(app, preview)
        assertEquals(2, dao.items().first().size)
        assertEquals(
            "Keep this current name",
            dao.items().first().first { it.id == "existing" }.name,
        )
        assertEquals("Current home", dao.households().first().single().name)
        val saved = dao.attachments().first().single()
        assertNotEquals("incoming", saved.file)
        assertEquals(file.readText(), File(app.filesDir, "attachments/${saved.file}").readText())
    }
}
