package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ReminderItem
import com.example.data.time.ReminderTime
import java.util.Calendar

enum class NotificationPostResult { POSTED, BLOCKED, DISABLED, FAILED }

object NotificationHelper {
    const val CHANNEL_REMINDERS_ID = "flowmind_reminders_channel"
    const val CHANNEL_URGENT_ID = "flowmind_urgent_channel"
    const val CHANNEL_STANDARD_ID = "flowmind_standard_alerts_v2"
    private const val TEST_NOTIFICATION_ID = -2001

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Existing channel IDs retain the user's Android notification preferences.
        manager.createNotificationChannel(NotificationChannel(CHANNEL_REMINDERS_ID, "FlowMind reminders", NotificationManager.IMPORTANCE_HIGH))
        manager.createNotificationChannel(NotificationChannel(CHANNEL_URGENT_ID, "Reminder banners", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders with heads-up alerts"; enableVibration(true)
        })
        manager.createNotificationChannel(NotificationChannel(CHANNEL_STANDARD_ID, "Standard reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Reminders without a heads-up banner"; enableVibration(true)
        })
    }

    fun hasRuntimePermission(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun canPost(context: Context, headsUp: Boolean = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE).getBoolean("notification_heads_up", true)): Boolean {
        if (!hasRuntimePermission(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(if (headsUp) CHANNEL_URGENT_ID else CHANNEL_STANDARD_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
            if (Build.VERSION.SDK_INT >= 28 && channel?.group != null && manager.getNotificationChannelGroup(channel.group)?.isBlocked == true) return false
        }
        return true
    }

    fun soundAllowed(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        return prefs.getBoolean("notification_sound_enabled", true) && !(prefs.getBoolean("smart_quiet_hours", true) && (hour >= 22 || hour < 7))
    }

    fun showReminderNotification(context: Context, reminder: ReminderItem, soundEnabled: Boolean = soundAllowed(context),
        headsUp: Boolean = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE).getBoolean("notification_heads_up", true)): NotificationPostResult {
        if (!context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE).getBoolean("notifications_enabled", true)) return NotificationPostResult.DISABLED
        return post(context, reminder.id.toInt(), build(context, reminder, soundEnabled, headsUp, includeActions = true), headsUp)
    }

    private fun build(context: Context, reminder: ReminderItem, soundEnabled: Boolean, headsUp: Boolean, includeActions: Boolean): Notification {
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        val due = ReminderTime.label(reminder.dueDateMillis, prefs.getString("time_protocol", "12h") ?: "12h")
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (includeActions) putExtra("reminder_id", reminder.id)
        }
        val content = PendingIntent.getActivity(context, reminder.id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, if (headsUp) CHANNEL_URGENT_ID else CHANNEL_STANDARD_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.notes.ifBlank { "$due · ${reminder.category}" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (reminder.notes.isBlank()) due else "${reminder.notes}\n\n$due"))
            .setSubText(reminder.category)
            .setPriority(if (headsUp) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setWhen(reminder.dueDateMillis)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(content)
        if (soundEnabled) builder.setDefaults(NotificationCompat.DEFAULT_ALL) else builder.setSilent(true)
        if (includeActions) {
            fun actionIntent(action: String) = PendingIntent.getBroadcast(context, reminder.id.toInt(),
                Intent(context, ReminderNotificationReceiver::class.java).apply {
                    this.action = action
                    putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(ReminderNotificationReceiver.EXTRA_DUE_MILLIS, reminder.dueDateMillis)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(R.drawable.ic_notification, "Done", actionIntent(ReminderNotificationReceiver.ACTION_COMPLETE_REMINDER))
                .addAction(R.drawable.ic_notification, "Snooze ${prefs.getString("snooze_latency", "1 hour")}", actionIntent(ReminderNotificationReceiver.ACTION_SNOOZE_REMINDER))
        }
        return builder.build()
    }

    private fun post(context: Context, id: Int, notification: Notification, headsUp: Boolean): NotificationPostResult {
        initNotificationChannels(context)
        if (!canPost(context, headsUp)) return NotificationPostResult.BLOCKED
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            NotificationPostResult.POSTED
        } catch (error: SecurityException) {
            NotificationPostResult.BLOCKED
        } catch (error: Exception) {
            Log.e("FlowMindAlerts", "Couldn't post reminder notification", error)
            NotificationPostResult.FAILED
        }
    }

    fun sendTestNotification(context: Context): NotificationPostResult {
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        val headsUp = prefs.getBoolean("notification_heads_up", true)
        val reminder = ReminderItem(id = TEST_NOTIFICATION_ID.toLong(), title = "A little nudge from FlowMind",
            notes = "This tests notification display. Reminder timing also depends on alarms and your phone's battery settings.", category = "Test")
        return post(context, TEST_NOTIFICATION_ID, build(context, reminder, soundAllowed(context), headsUp, includeActions = false), headsUp)
    }

    fun dismissNotification(context: Context, notificationId: Int) { NotificationManagerCompat.from(context).cancel(notificationId) }
}
