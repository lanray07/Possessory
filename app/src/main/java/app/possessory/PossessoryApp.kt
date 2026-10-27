package app.possessory

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.*
import app.possessory.data.PossessoryDatabase
import app.possessory.services.ReminderWorker
import java.util.concurrent.TimeUnit

val android.content.Context.settings by preferencesDataStore(name = "preferences")

open class PossessoryApp : Application() {
  override fun attachBaseContext(base: android.content.Context) {
    super.attachBaseContext(AppLanguage.apply(base))
  }

  val db by lazy {
    Room.databaseBuilder(this, PossessoryDatabase::class.java, "possessory.db").build()
  }
  val billing by lazy { app.possessory.services.AmazonBilling(this) }

  override fun onCreate() {
    super.onCreate()
    WorkManager.getInstance(this)
      .enqueueUniquePeriodicWork(
        "household-reminders",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build(),
      )
  }
}
