package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ReminderItem

object NotificationHelper {

    const val CHANNEL_REMINDERS_ID = "flowmind_reminders_channel"
    const val CHANNEL_URGENT_ID = "flowmind_urgent_channel"
    private const val CHANNEL_REMINDERS_NAME = "FlowMind Reminders"
    private const val CHANNEL_URGENT_NAME = "FlowMind Urgent Alerts"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Standard reminder channel
            val standardChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                CHANNEL_REMINDERS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard upcoming reminder notifications and schedule alerts"
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
            }

            // Urgent reminder channel with maximum importance
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT_ID,
                CHANNEL_URGENT_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority reminder alerts with prominent heads-up presentation"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            }

            notificationManager.createNotificationChannel(standardChannel)
            notificationManager.createNotificationChannel(urgentChannel)
        }
    }

    fun showReminderNotification(
        context: Context,
        reminder: ReminderItem,
        soundEnabled: Boolean = true,
        headsUp: Boolean = true
    ) {
        initNotificationChannels(context)

        val channelId = if (reminder.priority == "HIGH" || headsUp) {
            CHANNEL_URGENT_ID
        } else {
            CHANNEL_REMINDERS_ID
        }

        // Tap content intent -> opens MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminder.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark Done
        val doneIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_COMPLETE_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 15m
        val snoozeIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_SNOOZE_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (reminder.notes.isNotBlank()) {
            reminder.notes
        } else {
            "Due: ${reminder.dueTimeString} • Category: ${reminder.category}"
        }

        val priorityBadge = if (reminder.priority == "HIGH") "[Urgent] " else ""

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ $priorityBadge${reminder.title}")
            .setContentText(contentText)
            .setSubText(reminder.category)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (reminder.notes.isNotBlank()) {
                    "${reminder.notes}\n\nScheduled for ${reminder.dueTimeString} • Priority: ${reminder.priority}"
                } else {
                    "Scheduled for ${reminder.dueTimeString} • Priority: ${reminder.priority}"
                }
            ))
            .setPriority(
                if (reminder.priority == "HIGH") NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "✓ Mark Done", donePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "⏰ Snooze 15m", snoozePendingIntent)

        if (!soundEnabled) {
            notificationBuilder.setSilent(true)
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(reminder.id.toInt(), notificationBuilder.build())
        } catch (e: SecurityException) {
            // Notifications permission not granted by user
        }
    }

    fun sendTestNotification(context: Context) {
        initNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            99999,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_URGENT_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ FlowMind Notification Engine")
            .setContentText("✅ Notifications are verified & delivery is active!")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your notification engine is calibrated.\nAlarms, vibration, and actionable badges are delivered when reminder due times approach."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(99999, notification)
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}
