package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.model.ReminderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReminderNotificationScheduler {

    fun scheduleReminder(
        context: Context,
        reminder: ReminderItem,
        leadMinutes: Int = 0
    ) {
        if (reminder.isCompleted || reminder.isArchived) {
            cancelReminder(context, reminder.id)
            return
        }

        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) {
            return
        }

        val leadTimeMillis = leadMinutes * 60 * 1000L
        val triggerTime = reminder.dueDateMillis - leadTimeMillis
        val currentTime = System.currentTimeMillis()

        // If time already passed by more than 1 minute, don't trigger stale alert
        if (triggerTime < currentTime - 60_000L) {
            return
        }

        val effectiveTriggerTime = if (triggerTime <= currentTime) {
            currentTime + 1500L // Trigger in 1.5 seconds if due right now
        } else {
            triggerTime
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderNotificationReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderNotificationReceiver.EXTRA_CATEGORY, reminder.category)
            putExtra(ReminderNotificationReceiver.EXTRA_PRIORITY, reminder.priority)
            putExtra(ReminderNotificationReceiver.EXTRA_NOTES, reminder.notes)
            putExtra(ReminderNotificationReceiver.EXTRA_DUE_TIME, reminder.dueTimeString)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminder.id)
        }
        val showIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // AlarmClockInfo is Android's highest-precision wakeup mechanism, bypassing Doze throttling
            val alarmClockInfo = AlarmManager.AlarmClockInfo(effectiveTriggerTime, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            effectiveTriggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            effectiveTriggerTime,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        effectiveTriggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        effectiveTriggerTime,
                        pendingIntent
                    )
                }
            } catch (e2: Exception) {
                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        effectiveTriggerTime,
                        pendingIntent
                    )
                } catch (e3: Exception) {
                    // Handled
                }
            }
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllActiveReminders(context: Context) {
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        val leadMinutes = prefs.getInt("notification_lead_minutes", 0)

        if (!notificationsEnabled) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeList = db.reminderDao().getActiveRemindersList()
                activeList.forEach { reminder ->
                    scheduleReminder(context, reminder, leadMinutes)
                }
            } catch (e: Exception) {
                // Log or handle safely
            }
        }
    }
}
