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
        Room.databaseBuilder(this, PossessoryDatabase::class.java, "possessory.db")
            .addMigrations(PossessoryDatabase.MIGRATION_1_2)
            .build()
    }
    val billing by lazy { app.possessory.services.AmazonBilling(this) }

    override fun onCreate() {
        super.onCreate()
        // Previous-process restore previews never contain the authoritative inventory.
        java.io.File(cacheDir, "backup-previews").deleteRecursively()
        cacheDir
            .listFiles()
            ?.filter { it.name.startsWith("restore-") && it.name.endsWith(".zip") }
            ?.forEach { it.delete() }
        RegionalPreferences.dateStyle =
            getSharedPreferences("regional", MODE_PRIVATE).getString("dateStyle", "regional")
                ?: "regional"
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "household-reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build(),
            )
    }
}
