package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderDao
import com.example.data.model.ReminderItem

/** EXACT and APPROXIMATE describe registration, not proof of eventual phone delivery. */
enum class ReminderScheduleResult { EXACT, APPROXIMATE, DISABLED, ALREADY_DELIVERED, EXPIRED, FAILED }

object ReminderNotificationScheduler {
    fun scheduleReminder(context: Context, reminder: ReminderItem, leadMinutes: Int = 0): ReminderScheduleResult {
        cancelReminder(context, reminder.id)
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        if (reminder.isCompleted || reminder.isArchived || !prefs.getBoolean("notifications_enabled", true)) {
            return ReminderScheduleResult.DISABLED
        }
        val state = NotificationState(context)
        if (state.wasDelivered(reminder.id, reminder.dueDateMillis)) return ReminderScheduleResult.ALREADY_DELIVERED
        val trigger = NotificationTiming.triggerAt(reminder.dueDateMillis, leadMinutes, System.currentTimeMillis(), state.isSnoozed(reminder.id, reminder.dueDateMillis))
            ?: return ReminderScheduleResult.EXPIRED
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderNotificationReceiver.EXTRA_DUE_MILLIS, reminder.dueDateMillis)
            putExtra(ReminderNotificationReceiver.EXTRA_TRIGGER_MILLIS, trigger)
        }
        val pending = PendingIntent.getBroadcast(context, reminder.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try {
            if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                ReminderScheduleResult.EXACT
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                ReminderScheduleResult.APPROXIMATE
            }
        } catch (error: SecurityException) {
            // Access can change between the permission check and the registration call.
            try {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                ReminderScheduleResult.APPROXIMATE
            } catch (fallbackError: Exception) {
                Log.e("FlowMindAlarms", "Alarm registration failed", fallbackError)
                ReminderScheduleResult.FAILED
            }
        } catch (error: Exception) {
            Log.e("FlowMindAlarms", "Alarm registration failed", error)
            ReminderScheduleResult.FAILED
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER
        }
        val pending = PendingIntent.getBroadcast(context, reminderId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
        pending.cancel()
    }

    suspend fun rescheduleAllActiveReminders(context: Context, dao: ReminderDao = AppDatabase.getDatabase(context).reminderDao()) {
        // Awaited by goAsync receivers and lifecycle-owned callers; no detached coroutine.
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        dao.getActiveRemindersList().forEach { reminder ->
            scheduleReminder(context, reminder, prefs.getInt("notification_lead_minutes", 0))
            if (!prefs.getBoolean("notifications_enabled", true)) NotificationHelper.dismissNotification(context, reminder.id.toInt())
        }
    }
}
