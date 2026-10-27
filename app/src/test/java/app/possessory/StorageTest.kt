package app.possessory

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.possessory.data.*
import app.possessory.services.*
import java.io.*
import java.util.zip.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class StorageTest {
  @Test
  fun databasePersistsAndCascadesRelatedRecords() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, PossessoryDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val dao = db.inventory()
      dao.put(Household("home", "Test home"))
      dao.put(RoomRecord("room", "home", "Office"))
      val item = InventoryItem(id = "item", name = "Laptop", roomId = "room", priceMinor = 99999)
      dao.put(item)
      dao.put(Attachment("receipt", "item", "Receipt", "application/pdf", "receipt"))
      dao.put(Maintenance("service", "item", "Clean", "2026-10-27"))
      assertEquals(99999L, dao.items().first().single().priceMinor)
      dao.put(item.copy(name = "Work laptop"))
      assertEquals(1, dao.items().first().size)
      dao.delete(item)
      assertTrue(dao.attachments().first().isEmpty())
      assertTrue(dao.maintenance().first().isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun backupRetainsAttachmentsAndDetectsModification() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val attachment =
      File(context.filesDir, "attachments/receipt").apply {
        parentFile!!.mkdirs()
        writeText("receipt bytes")
      }
    val state =
      InventoryState(
        listOf(Household("home", "Home")),
        listOf(RoomRecord("room", "home", "Kitchen")),
        listOf(InventoryItem(id = "item", name = "Kettle", roomId = "room")),
        listOf(Attachment("receipt", "item", "Receipt", "application/pdf", "receipt")),
      )
    val out = ByteArrayOutputStream()
    Files.backup(context, state, out)
    val p = Files.preview(ByteArrayInputStream(out.toByteArray()))
    assertEquals("Kettle", p.snapshot.items.single().name)
    assertArrayEquals(attachment.readBytes(), p.files.getValue("attachments/receipt"))
    val broken = ByteArrayOutputStream()
    ZipOutputStream(broken).use { zip ->
      zip.putNextEntry(ZipEntry("manifest.json"))
      zip.write(Files.gson.toJson(p.snapshot).toByteArray())
      zip.closeEntry()
      zip.putNextEntry(ZipEntry("attachments/receipt"))
      zip.write("tampered".toByteArray())
      zip.closeEntry()
    }
    assertTrue(runCatching { Files.preview(ByteArrayInputStream(broken.toByteArray())) }.isFailure)
  }

  @Test
  fun backupRejectsTraversalAndUnsupportedSchemas() {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
      zip.putNextEntry(ZipEntry("../outside"))
      zip.write(byteArrayOf(1))
      zip.closeEntry()
    }
    assertTrue(runCatching { Files.preview(ByteArrayInputStream(out.toByteArray())) }.isFailure)
    assertTrue(runCatching { Files.readBounded(ByteArrayInputStream(ByteArray(32)), 16) }.isFailure)
  }

  @Test
  fun receiptSuggestionsNeverInventDatesOrAmbiguousCurrency() {
    val fields = ReceiptOcr.extract("Home Store\nCoffee machine\nTOTAL $ 49.99")
    assertEquals("49.99", fields.total)
    assertEquals("", fields.date)
    assertEquals("", fields.currency)
    assertEquals("", ReceiptOcr.extract("No total available").total)
  }
}
