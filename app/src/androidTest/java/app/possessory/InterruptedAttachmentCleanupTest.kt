package app.possessory

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Removes only the uniquely named disposable attachment fixtures after an interrupted QA run. */
@RunWith(AndroidJUnit4::class)
class InterruptedAttachmentCleanupTest {
  @Test
  fun removeOwnedInterruptedFixtures() = runBlocking {
    org.junit.Assume.assumeTrue(
        BuildConfig.REVIEW_MODE &&
            InstrumentationRegistry.getArguments().getString("cleanupAttachmentFixtures") == "true"
    )
    val app =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as PossessoryApp
    assertEquals("app.possessory.review", app.packageName)
    val dao = app.db.inventory()
    val pattern = Regex("qa-attachment-[0-9a-f-]{36}")
    val homes =
        dao.households().first().filter {
          pattern.matches(it.id) && it.name == "QA disposable household"
        }
    homes.forEach { home ->
      val rooms =
          dao.rooms().first().filter {
            it.id == home.id && it.householdId == home.id && it.name == "QA disposable room"
          }
      rooms.forEach { room ->
        dao.items()
            .first()
            .filter { it.id == home.id && it.roomId == room.id && it.name == "QA attachment check" }
            .forEach { item ->
              dao.attachments()
                  .first()
                  .filter { it.itemId == item.id }
                  .forEach { File(app.filesDir, "attachments/${it.file}").delete() }
              dao.delete(item)
            }
        app.db.openHelper.writableDatabase.execSQL(
            "DELETE FROM rooms WHERE id = ?",
            arrayOf(room.id),
        )
      }
      app.db.openHelper.writableDatabase.execSQL(
          "DELETE FROM households WHERE id = ?",
          arrayOf(home.id),
      )
    }
    assertTrue(
        dao.households().first().none {
          pattern.matches(it.id) && it.name == "QA disposable household"
        }
    )
  }
}
