package app.possessory.data

import androidx.room.*
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "households")
data class Household(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val currency: String = "GBP",
)

@Entity(
    tableName = "rooms",
    foreignKeys =
        [
            ForeignKey(
                entity = Household::class,
                parentColumns = ["id"],
                childColumns = ["householdId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("householdId")],
)
data class RoomRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val householdId: String,
    val name: String,
    val kind: String = "Other",
    @ColumnInfo(defaultValue = "''") val photoFile: String = "",
)

@Entity(
    tableName = "items",
    foreignKeys =
        [
            ForeignKey(
                entity = RoomRecord::class,
                parentColumns = ["id"],
                childColumns = ["roomId"],
                onDelete = ForeignKey.RESTRICT,
            )
        ],
    indices = [Index("roomId")],
)
data class InventoryItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val roomId: String,
    val category: String = "Other",
    val description: String = "",
    val brand: String = "",
    val model: String = "",
    val serial: String = "",
    val purchaseDate: String = "",
    val priceMinor: Long? = null,
    val currency: String = "GBP",
    val retailer: String = "",
    val estimateMinor: Long? = null,
    val warrantyStart: String = "",
    val warrantyEnd: String = "",
    val warrantyProvider: String = "",
    val warrantyType: String = "Manufacturer",
    val warrantyNotes: String = "",
    val reminderDays: Int = 30,
    @ColumnInfo(defaultValue = "''") val reminderOffsets: String = "",
    @ColumnInfo(defaultValue = "''") val reminderDates: String = "",
    val notes: String = "",
    val tags: String = "",
    val condition: String = "Good",
    val status: String = "Active",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "attachments",
    foreignKeys =
        [
            ForeignKey(
                entity = InventoryItem::class,
                parentColumns = ["id"],
                childColumns = ["itemId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("itemId")],
)
data class Attachment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val name: String,
    val mime: String,
    val file: String,
    val kind: String = "Receipt",
    @ColumnInfo(defaultValue = "''") val maintenanceId: String = "",
)

@Entity(
    tableName = "maintenance",
    foreignKeys =
        [
            ForeignKey(
                entity = InventoryItem::class,
                parentColumns = ["id"],
                childColumns = ["itemId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("itemId")],
)
data class Maintenance(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val type: String,
    val date: String,
    val nextDate: String = "",
    val repeatDays: Int = 0,
    val notes: String = "",
    val costMinor: Long? = null,
)

@Dao
interface InventoryDao {
    @Query("SELECT * FROM attachments WHERE itemId=:itemId")
    suspend fun attachmentsForItem(itemId: String): List<Attachment>

    @Query("SELECT COUNT(*) FROM households") suspend fun householdCount(): Int

    @Query("SELECT COUNT(*) FROM items") suspend fun itemCount(): Int

    @Query("SELECT * FROM items WHERE id=:id") suspend fun item(id: String): InventoryItem?

    @Query("SELECT EXISTS(SELECT 1 FROM items WHERE id=:id)")
    suspend fun hasItem(id: String): Boolean

    @Query("SELECT * FROM households ORDER BY name") fun households(): Flow<List<Household>>

    @Query("SELECT * FROM rooms ORDER BY name") fun rooms(): Flow<List<RoomRecord>>

    @Query("SELECT * FROM items ORDER BY createdAt DESC") fun items(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM attachments") fun attachments(): Flow<List<Attachment>>

    @Query("SELECT * FROM maintenance ORDER BY nextDate") fun maintenance(): Flow<List<Maintenance>>

    @Upsert suspend fun put(value: Household)

    @Upsert suspend fun put(value: RoomRecord)

    @Upsert suspend fun put(value: InventoryItem)

    @Upsert suspend fun put(value: Attachment)

    @Upsert suspend fun put(value: Maintenance)

    @Delete suspend fun delete(value: InventoryItem)

    @Delete suspend fun delete(value: Attachment)

    @Delete suspend fun delete(value: Maintenance)

    @Query("DELETE FROM maintenance") suspend fun clearMaintenance()

    @Query("DELETE FROM attachments") suspend fun clearAttachments()

    @Query("DELETE FROM items") suspend fun clearItems()

    @Query("DELETE FROM rooms") suspend fun clearRooms()

    @Query("DELETE FROM households") suspend fun clearHouseholds()
}

@Database(
    entities =
        [
            Household::class,
            RoomRecord::class,
            InventoryItem::class,
            Attachment::class,
            Maintenance::class,
        ],
    version = 2,
    exportSchema = true,
)
abstract class PossessoryDatabase : RoomDatabase() {
    abstract fun inventory(): InventoryDao

    companion object {
        val MIGRATION_1_2 =
            object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE rooms ADD COLUMN photoFile TEXT NOT NULL DEFAULT ''")
                    db.execSQL(
                        "ALTER TABLE items ADD COLUMN reminderOffsets TEXT NOT NULL DEFAULT ''"
                    )
                    db.execSQL(
                        "ALTER TABLE items ADD COLUMN reminderDates TEXT NOT NULL DEFAULT ''"
                    )
                    db.execSQL(
                        "ALTER TABLE attachments ADD COLUMN maintenanceId TEXT NOT NULL DEFAULT ''"
                    )
                }
            }
    }
}
