package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ReminderItem
import com.example.notification.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/** Exercises the production receiver handler, Room mutations, alarm manager, and notification builder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationDeliveryTest {
    private lateinit var app: Application
    private lateinit var database: AppDatabase
    private val receiver = ReminderNotificationReceiver()
    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("flowmind_notification_state", Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.initNotificationChannels(app)
        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun close() { database.close() }
    private fun action(action: String, reminder: ReminderItem) = Intent(app, ReminderNotificationReceiver::class.java).apply {
        this.action = action
        putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
        putExtra(ReminderNotificationReceiver.EXTRA_DUE_MILLIS, reminder.dueDateMillis)
    }
    private suspend fun insert(due: Long = System.currentTimeMillis() - 2_000): ReminderItem {
        val item = ReminderItem(title = "Actual test reminder", dueDateMillis = due)
        return item.copy(id = database.reminderDao().insertReminder(item))
    }

    @Test fun `trigger posts an actionable notification and repeated delivery is suppressed`() = runBlocking {
        val item = insert()
        val intent = action(ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER, item)
        receiver.handleReminderIntent(app, intent, database.reminderDao())
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = requireNotNull(shadowOf(manager).getNotification(item.id.toInt()))
        assertEquals(2, posted.actions?.size ?: 0)
        assertEquals(R.drawable.ic_notification, posted.smallIcon?.resId ?: 0)
        assertTrue(NotificationState(app).wasDelivered(item.id, item.dueDateMillis))
        manager.cancel(item.id.toInt())
        receiver.handleReminderIntent(app, intent, database.reminderDao())
        assertNull(shadowOf(manager).getNotification(item.id.toInt()))
    }
    @Test fun `blocked permission is reported and does not mark the reminder delivered`() = runBlocking {
        val item = insert()
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(NotificationPostResult.BLOCKED, NotificationHelper.sendTestNotification(app))
        receiver.handleReminderIntent(app, action(ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER, item), database.reminderDao())
        assertFalse(NotificationState(app).wasDelivered(item.id, item.dueDateMillis))
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        receiver.handleReminderIntent(app, action(ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER, item), database.reminderDao())
        assertTrue(NotificationState(app).wasDelivered(item.id, item.dueDateMillis))
    }
    @Test fun `blocked channel is reported instead of pretending the test was sent`() {
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(NotificationHelper.CHANNEL_URGENT_ID, "Blocked", NotificationManager.IMPORTANCE_NONE))
        assertFalse(NotificationHelper.canPost(app))
        assertEquals(NotificationPostResult.BLOCKED, NotificationHelper.sendTestNotification(app))
    }
    @Test fun `stale alarm and stale done action cannot affect a rescheduled reminder`() = runBlocking {
        val item = insert()
        val updated = item.copy(dueDateMillis = item.dueDateMillis + 3_600_000)
        database.reminderDao().updateReminder(updated)
        receiver.handleReminderIntent(app, action(ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER, item), database.reminderDao())
        receiver.handleReminderIntent(app, action(ReminderNotificationReceiver.ACTION_COMPLETE_REMINDER, item), database.reminderDao())
        assertFalse(database.reminderDao().getReminderById(item.id)!!.isCompleted)
        assertFalse(NotificationState(app).wasDelivered(item.id, updated.dueDateMillis))
    }
    @Test fun `deleted reminders cannot be resurrected from old pending intent extras`() = runBlocking {
        val item = insert()
        database.reminderDao().deleteReminder(item)
        receiver.handleReminderIntent(app, action(ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER, item), database.reminderDao())
        assertEquals(0, database.reminderDao().getTotalCount())
        assertNull(shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).getNotification(item.id.toInt()))
    }
    @Test fun `snooze uses the selected duration and survives alarm reconstruction`() = runBlocking {
        app.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE).edit()
            .putString("snooze_latency", "30 min").putInt("notification_lead_minutes", 15).commit()
        val item = insert()
        val before = System.currentTimeMillis()
        receiver.handleReminderIntent(app, action(ReminderNotificationReceiver.ACTION_SNOOZE_REMINDER, item), database.reminderDao())
        val updated = database.reminderDao().getReminderById(item.id)!!
        assertTrue(updated.dueDateMillis >= before + 30 * 60_000L)
        assertTrue(updated.dueDateMillis <= System.currentTimeMillis() + 30 * 60_000L)
        assertTrue(NotificationState(app).isSnoozed(item.id, updated.dueDateMillis))
        ReminderNotificationScheduler.rescheduleAllActiveReminders(app, database.reminderDao())
        val alarms = shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(updated.dueDateMillis, alarms.single().triggerAtTime)
    }
    @Test fun `snooze and editing do not undo notification completion`() = runBlocking {
        val item = insert()
        database.reminderDao().changeCompletion(item.id, false, System.currentTimeMillis())
        assertNull(database.reminderDao().snoozeActiveReminder(item.id, item.dueDateMillis, 60_000, System.currentTimeMillis()))
        val edited = database.reminderDao().updateDetails(item.copy(title = "Edited later"))!!
        assertTrue(edited.isCompleted)
        assertNotNull(edited.completedAtMillis)
    }
    @Test fun `recovery re-registers future alarms and skips reminders already delivered`() = runBlocking {
        val item = insert(System.currentTimeMillis() + 3_600_000)
        ReminderNotificationScheduler.rescheduleAllActiveReminders(app, database.reminderDao())
        val alarm = shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        assertEquals(1, alarm.scheduledAlarms.size)
        NotificationState(app).markDelivered(item.id, item.dueDateMillis)
        ReminderNotificationScheduler.rescheduleAllActiveReminders(app, database.reminderDao())
        assertTrue(alarm.scheduledAlarms.isEmpty())
    }
    @Test fun `missing exact alarm access uses an explicit approximate fallback`() = runBlocking {
        val item = insert(System.currentTimeMillis() + 3_600_000)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertEquals(ReminderScheduleResult.APPROXIMATE, ReminderNotificationScheduler.scheduleReminder(app, item))
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertEquals(ReminderScheduleResult.EXACT, ReminderNotificationScheduler.scheduleReminder(app, item))
    }
}
