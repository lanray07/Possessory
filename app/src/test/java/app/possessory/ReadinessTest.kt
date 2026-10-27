package app.possessory

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.possessory.data.*
import app.possessory.domain.*
import app.possessory.services.Files
import com.google.gson.JsonParser
import java.io.*
import java.time.LocalDate
import java.util.zip.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ReadinessTest {
    @Test
    fun databaseV1UpgradePreservesRecordsAndAddsSafeDefaults() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "readiness-migration.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schemaFile = File("schemas/app.possessory.data.PossessoryDatabase/1.json")
        val schema =
            JsonParser.parseString(schemaFile.readText()).asJsonObject.getAsJsonObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            schema.getAsJsonArray("entities").forEach { element ->
                val entity = element.asJsonObject
                val table = entity.get("tableName").asString
                db.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
                entity.getAsJsonArray("indices")?.forEach { index ->
                    db.execSQL(
                        index.asJsonObject
                            .get("createSql")
                            .asString
                            .replace("\${TABLE_NAME}", table)
                    )
                }
            }
            db.execSQL(
                "INSERT INTO households(id,name,currency) VALUES('home','Existing home','GBP')"
            )
            db.execSQL(
                "INSERT INTO rooms(id,householdId,name,kind) VALUES('room','home','Kitchen','Kitchen')"
            )
            db.version = 1
        }
        val db =
            Room.databaseBuilder(context, PossessoryDatabase::class.java, name)
                .addMigrations(PossessoryDatabase.MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()
        try {
            assertEquals("Existing home", db.inventory().households().first().single().name)
            assertEquals("", db.inventory().rooms().first().single().photoFile)
            assertEquals(2, db.openHelper.writableDatabase.version)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun oldBackupsReceiveDefaultsWithoutLosingValues() {
        val snapshot =
            app.possessory.services.Snapshot(
                households = listOf(Household("home", "Home")),
                rooms = listOf(RoomRecord("room", "home", "Kitchen")),
                items = listOf(InventoryItem("item", "Kettle", "room", priceMinor = 2999)),
                attachments = emptyList(),
                maintenance = emptyList(),
                hashes = emptyMap(),
            )
        val tree = Files.gson.toJsonTree(snapshot).asJsonObject.apply { addProperty("schema", 1) }
        tree.getAsJsonArray("rooms").forEach { it.asJsonObject.remove("photoFile") }
        tree.getAsJsonArray("items").forEach {
            it.asJsonObject.remove("reminderOffsets")
            it.asJsonObject.remove("reminderDates")
        }
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(tree.toString().toByteArray())
            zip.closeEntry()
        }
        val loaded = Files.preview(ByteArrayInputStream(bytes.toByteArray())).snapshot
        assertEquals(2999L, loaded.items.single().priceMinor)
        assertEquals("", loaded.items.single().reminderOffsets)
        assertEquals("", loaded.rooms.single().photoFile)
    }

    @Test
    fun priceRangeNeverComparesDifferentCurrenciesOrUndatedRecords() {
        val rule =
            SearchCriteria(
                currency = "GBP",
                minimum = "10",
                maximum = "50",
                purchasedAfter = "2026-01-01",
                purchasedBefore = "2026-12-31",
            )
        rule.validate()
        val item =
            InventoryItem("item", "Kettle", "room", priceMinor = 2000, purchaseDate = "2026-10-01")
        assertTrue(rule.matches(item))
        assertFalse(rule.matches(item.copy(currency = "USD")))
        assertFalse(rule.matches(item.copy(purchaseDate = "")))
        assertFalse(rule.matches(item.copy(priceMinor = 5001)))
        assertTrue(runCatching { rule.copy(minimum = "60").validate() }.isFailure)
    }

    @Test
    fun customRemindersFireOnlyOnSelectedDatesAndForActiveItems() {
        val item =
            InventoryItem(
                "item",
                "Kettle",
                "room",
                warrantyEnd = "2026-12-31",
                reminderOffsets = "30,14,7,0",
                reminderDates = "2026-10-27",
            )
        assertTrue(ReminderRules.due(item, LocalDate.parse("2026-10-27")))
        assertTrue(ReminderRules.due(item, LocalDate.parse("2026-12-17")))
        assertFalse(ReminderRules.due(item, LocalDate.parse("2026-12-18")))
        assertFalse(
            ReminderRules.due(item.copy(status = "Archived"), LocalDate.parse("2026-12-17"))
        )
        assertTrue(runCatching { ReminderRules.dates("2026-02-31") }.isFailure)
        assertTrue(
            ReminderRules.dueSince(
                item,
                LocalDate.parse("2026-12-16"),
                LocalDate.parse("2026-12-19"),
            )
        )
        assertFalse(
            ReminderRules.dueSince(
                item,
                LocalDate.parse("2026-12-18"),
                LocalDate.parse("2026-12-19"),
            )
        )
    }

    @Test
    fun savedPriceFiltersSurviveSwitchingInterfaceLanguage() {
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            val search = SearchCriteria(currency = "EUR", minimum = "10,50", maximum = "20,99")
            val restored =
                Files.gson.fromJson(Files.gson.toJson(search), SearchCriteria::class.java)
            java.util.Locale.setDefault(java.util.Locale.UK)
            restored.validate()
            assertTrue(
                restored.matches(
                    InventoryItem("x", "Camera", "room", currency = "EUR", priceMinor = 1500)
                )
            )
            assertFalse(
                restored.matches(
                    InventoryItem("y", "Camera", "room", currency = "EUR", priceMinor = 1050 - 1)
                )
            )
            assertTrue(
                runCatching { InventoryRules.amount("10.50", "EUR", java.util.Locale.GERMANY) }
                    .isFailure
            )
        } finally {
            java.util.Locale.setDefault(original)
        }
    }

    @Test
    fun voicePreservesNamesAndReturnsAnIntentWithoutMutatingData() {
        assertEquals(
            VoiceIntent.WarrantiesThisMonth,
            VoiceParser.parse("Which warranties expire this month?"),
        )
        assertEquals(
            VoiceIntent.AddNamed("Samsung television", "living room"),
            VoiceParser.parse("Add a Samsung television to the living room."),
        )
        assertEquals(
            VoiceIntent.Move("Printer", "office"),
            VoiceParser.parse("Move Printer to the office"),
        )
        assertEquals(
            VoiceIntent.Count("bedroom"),
            VoiceParser.parse("How many items are in the bedroom?"),
        )
        val spanish =
            VoiceTerms(find = "buscar", room = "mostrar artículos en", move = "mover", to = "a")
        assertEquals(VoiceIntent.Find("Mi cámara"), VoiceParser.parse("buscar Mi cámara", spanish))
        assertEquals(
            VoiceIntent.Move("Impresora", "Despacho"),
            VoiceParser.parse("mover Impresora a Despacho", spanish),
        )
        assertEquals(VoiceIntent.Unknown, VoiceParser.parse("delete all my belongings"))
    }
}
