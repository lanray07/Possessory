package app.possessory

import androidx.test.core.app.ApplicationProvider
import app.possessory.data.*
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
    assertEquals("Keep this current name", dao.items().first().first { it.id == "existing" }.name)
    assertEquals("Current home", dao.households().first().single().name)
    val saved = dao.attachments().first().single()
    assertNotEquals("incoming", saved.file)
    assertEquals(file.readText(), File(app.filesDir, "attachments/${saved.file}").readText())
  }
}
