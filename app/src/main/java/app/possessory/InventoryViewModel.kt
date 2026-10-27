package app.possessory

import android.app.Application
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import app.possessory.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryState(
  val households: List<Household> = emptyList(),
  val rooms: List<RoomRecord> = emptyList(),
  val items: List<InventoryItem> = emptyList(),
  val attachments: List<Attachment> = emptyList(),
  val maintenance: List<Maintenance> = emptyList(),
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
  val app = application as PossessoryApp
  val dao = app.db.inventory()
  val state =
    combine(dao.households(), dao.rooms(), dao.items(), dao.attachments(), dao.maintenance()) {
        h,
        r,
        i,
        a,
        m ->
        InventoryState(h, r, i, a, m)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryState())
  val preferences =
    app.settings.data.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyPreferences(),
    )
  val error = MutableStateFlow<String?>(null)

  fun run(action: suspend () -> Unit) {
    viewModelScope.launch {
      runCatching { action() }.onFailure { error.value = it.message ?: "Operation failed" }
    }
  }

  fun setting(key: String, value: String) = run {
    app.settings.edit { it[stringPreferencesKey(key)] = value }
  }

  fun household(name: String, currency: String) = run {
    require(name.isNotBlank())
    app.db.withTransaction {
      require(app.billing.state.value.premium || dao.householdCount() == 0) {
        "premium_unavailable"
      }
      val home = Household(name = name.trim(), currency = currency)
      dao.put(home)
      dao.put(
        RoomRecord(
          householdId = home.id,
          name = AppLanguage.apply(app).getString(R.string.living_room),
          kind = "Living Room",
        )
      )
    }
  }

  fun room(home: String, name: String, kind: String = "Other") = run {
    require(name.isNotBlank())
    dao.put(RoomRecord(householdId = home, name = name.trim(), kind = kind))
  }

  fun save(item: InventoryItem) = run {
    require(item.name.isNotBlank())
    app.db.withTransaction {
      require(app.billing.state.value.premium || dao.hasItem(item.id) || dao.itemCount() < 30) {
        "premium_unavailable"
      }
      dao.put(item.copy(updatedAt = System.currentTimeMillis()))
    }
  }

  fun delete(item: InventoryItem) = run {
    val files =
      app.db.withTransaction {
        val files =
          dao.attachmentsForItem(item.id).map {
            java.io.File(app.filesDir, "attachments/${it.file}")
          }
        dao.delete(item)
        files
      }
    files.forEach { it.delete() }
  }

  fun erase(onComplete: () -> Unit = {}) = run {
    app.db.withTransaction {
      dao.clearMaintenance()
      dao.clearAttachments()
      dao.clearItems()
      dao.clearRooms()
      dao.clearHouseholds()
    }
    java.io.File(app.filesDir, "attachments").deleteRecursively()
    app.settings.edit { it.clear() }
    app
      .getSharedPreferences(AppLanguage.PREFERENCES, android.content.Context.MODE_PRIVATE)
      .edit()
      .clear()
      .commit()
    onComplete()
  }
}
