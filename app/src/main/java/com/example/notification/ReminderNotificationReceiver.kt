package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.flowmind.ACTION_TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.example.flowmind.ACTION_COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.flowmind.ACTION_SNOOZE_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.reminderDao()
                val reminder = dao.getReminderById(reminderId)

                when (intent.action) {
                    ACTION_TRIGGER_REMINDER -> {
                        if (reminder != null && !reminder.isCompleted && !reminder.isArchived) {
                            val prefs = context.getSharedPreferences(
                                "flowmind_settings_prefs",
                                Context.MODE_PRIVATE
                            )
                            val notificationsEnabled =
                                prefs.getBoolean("notifications_enabled", true)
                            val soundEnabled =
                                prefs.getBoolean("notification_sound_enabled", true)
                            val headsUp =
                                prefs.getBoolean("notification_heads_up", true)

                            if (notificationsEnabled) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    reminder = reminder,
                                    soundEnabled = soundEnabled,
                                    headsUp = headsUp
                                )
                            }
                        }
                    }

                    ACTION_COMPLETE_REMINDER -> {
                        if (reminder != null) {
                            val updated = reminder.copy(
                                isCompleted = true,
                                completedAtMillis = System.currentTimeMillis()
                            )
                            dao.updateReminder(updated)
                            NotificationHelper.dismissNotification(context, reminder.id.toInt())
                        }
                    }

                    ACTION_SNOOZE_REMINDER -> {
                        if (reminder != null) {
                            val snoozeMillis = 15 * 60 * 1000L // 15 min
                            val updated = reminder.copy(
                                dueDateMillis = System.currentTimeMillis() + snoozeMillis,
                                dueTimeString = "+15m Snoozed"
                            )
                            dao.updateReminder(updated)
                            NotificationHelper.dismissNotification(context, reminder.id.toInt())

                            // Schedule alarm in 15 minutes
                            ReminderNotificationScheduler.scheduleReminder(
                                context = context,
                                reminder = updated,
                                leadMinutes = 0
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Log or handle safely
            } finally {
                pendingResult.finish()
            }
        }
    }
}
