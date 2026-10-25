package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderDao
import com.example.data.time.ReminderTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS || intent.getLongExtra(EXTRA_REMINDER_ID, -1) <= 0) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000) { handleReminderIntent(appContext, intent) }
            } catch (error: Exception) {
                Log.e("FlowMindAlerts", "Couldn't process reminder action", error)
            } finally { pending.finish() }
        }
    }

    internal suspend fun handleReminderIntent(context: Context, intent: Intent,
        dao: ReminderDao = AppDatabase.getDatabase(context).reminderDao()) = actionMutex.withLock {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        val reminder = dao.getReminderById(id) ?: return@withLock
        if (reminder.isCompleted || reminder.isArchived) return@withLock
        // A queued alarm or old notification action must not mutate a newly edited reminder.
        if (intent.hasExtra(EXTRA_DUE_MILLIS) && intent.getLongExtra(EXTRA_DUE_MILLIS, 0) != reminder.dueDateMillis) return@withLock
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        val state = NotificationState(context)
        when (intent.action) {
            ACTION_TRIGGER_REMINDER -> {
                if (!prefs.getBoolean("notifications_enabled", true) || state.wasDelivered(id, reminder.dueDateMillis)) return@withLock
                val now = System.currentTimeMillis()
                if (reminder.dueDateMillis < now - NotificationTiming.CATCH_UP_WINDOW_MILLIS) return@withLock
                val expected = intent.getLongExtra(EXTRA_TRIGGER_MILLIS,
                    reminder.dueDateMillis - if (state.isSnoozed(id, reminder.dueDateMillis)) 0 else prefs.getInt("notification_lead_minutes", 0) * 60_000L)
                if (expected > now + 5_000) {
                    ReminderNotificationScheduler.scheduleReminder(context, reminder, prefs.getInt("notification_lead_minutes", 0))
                    return@withLock
                }
                if (NotificationHelper.showReminderNotification(context, reminder) == NotificationPostResult.POSTED) {
                    state.markDelivered(id, reminder.dueDateMillis)
                }
            }
            ACTION_COMPLETE_REMINDER -> {
                val change = dao.changeCompletion(id, false, System.currentTimeMillis()) ?: return@withLock
                ReminderNotificationScheduler.cancelReminder(context, id)
                NotificationHelper.dismissNotification(context, id.toInt())
                change.next?.let { ReminderNotificationScheduler.scheduleReminder(context, it, prefs.getInt("notification_lead_minutes", 0)) }
            }
            ACTION_SNOOZE_REMINDER -> {
                val duration = ReminderTime.snoozeMillis(prefs.getString("snooze_latency", "1 hour") ?: "1 hour")
                val updated = dao.snoozeActiveReminder(id, reminder.dueDateMillis, duration, System.currentTimeMillis()) ?: return@withLock
                state.markSnoozed(id, updated.dueDateMillis)
                NotificationHelper.dismissNotification(context, id.toInt())
                ReminderNotificationScheduler.scheduleReminder(context, updated, 0)
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.flowmind.ACTION_TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.example.flowmind.ACTION_COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.flowmind.ACTION_SNOOZE_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_DUE_MILLIS = "extra_due_millis"
        const val EXTRA_TRIGGER_MILLIS = "extra_trigger_millis"
        private val ACTIONS = setOf(ACTION_TRIGGER_REMINDER, ACTION_COMPLETE_REMINDER, ACTION_SNOOZE_REMINDER)
        private val actionMutex = Mutex()
    }
}
