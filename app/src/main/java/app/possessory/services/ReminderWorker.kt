package app.possessory.services

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.*
import app.possessory.*
import app.possessory.R
import app.possessory.domain.InventoryRules
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class ReminderWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    val app = applicationContext as PossessoryApp
    val localized = AppLanguage.apply(app)
    if (app.settings.data.first()[stringPreferencesKey("reminders")] != "on")
      return Result.success()
    if (
      Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    )
      return Result.success()
    val dao = app.db.inventory()
    val items = dao.items().first().filter { it.status == "Active" }
    val due =
      items.count { InventoryRules.warranty(it) == "soon" } +
        dao.maintenance().first().count { m ->
          m.itemId in items.map { it.id } &&
            m.nextDate.isNotBlank() &&
            runCatching { LocalDate.parse(m.nextDate) <= LocalDate.now() }.getOrDefault(false)
        }
    val manager = app.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      NotificationChannel(
        "reminders",
        localized.getString(R.string.reminders),
        NotificationManager.IMPORTANCE_DEFAULT,
      )
    )
    if (due > 0) {
      val pending =
        PendingIntent.getActivity(
          app,
          0,
          Intent(app, MainActivity::class.java),
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
      manager.notify(
        41,
        NotificationCompat.Builder(app, "reminders")
          .setSmallIcon(R.drawable.icon_foreground)
          .setContentTitle(localized.getString(R.string.app_name))
          .setContentText(localized.resources.getQuantityString(R.plurals.reminder_count, due, due))
          .setContentIntent(pending)
          .setAutoCancel(true)
          .build(),
      )
    } else manager.cancel(41)
    return Result.success()
  }
}
