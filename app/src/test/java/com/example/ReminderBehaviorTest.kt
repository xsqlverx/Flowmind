package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.ReminderExport
import com.example.data.local.AppDatabase
import com.example.data.model.ReminderItem
import com.example.data.repository.ReminderRepository
import com.example.data.nlp.LlmReminderClient
import com.example.notification.ReminderNotificationScheduler
import com.example.voice.ReminderVoiceInput
import android.speech.SpeechRecognizer
import android.app.AlarmManager
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderBehaviorTest {
    private lateinit var database: AppDatabase
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun close() { database.close() }

    @Test fun `completion is transactional and repeated taps cannot create duplicate recurrences`() = runBlocking {
        val dao = database.reminderDao()
        val now = System.currentTimeMillis()
        val id = dao.insertReminder(ReminderItem(title = "Read", dueDateMillis = now, recurrence = "DAILY"))
        val change = dao.changeCompletion(id, false, now)!!
        assertTrue(change.updated.isCompleted)
        assertNotNull(change.next)
        assertNull(dao.changeCompletion(id, false, now))
        assertEquals(2, dao.getTotalCount())
        assertEquals(1, dao.getCompletedCount())
    }
    @Test fun `edited alarm timestamp is persisted`() = runBlocking {
        val dao = database.reminderDao()
        val id = dao.insertReminder(ReminderItem(title = "Original", dueDateMillis = 1000L))
        dao.updateReminder(dao.getReminderById(id)!!.copy(title = "Edited", dueDateMillis = 2_000_000L))
        assertEquals(2_000_000L, dao.getReminderById(id)!!.dueDateMillis)
    }
    @Test fun `switching providers keeps credentials separate and migrates an existing key`() {
        val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("llm_provider", "Groq").putString("api_key", "old-example-key").commit()
        val repository = ReminderRepository(database.reminderDao(), context)
        assertEquals("old-example-key", repository.settings.value.apiKey)
        repository.updateLlmProvider("OpenAI")
        assertEquals("", repository.settings.value.apiKey)
        repository.updateApiKey("another-example-key")
        repository.updateLlmProvider("Groq")
        assertEquals("old-example-key", repository.settings.value.apiKey)
        assertFalse(repository.settings.value.isKeyVerified)
    }
    @Test fun `disabling notifications cancels an existing alarm`() {
        val reminder = ReminderItem(id = 18, title = "Test", dueDateMillis = System.currentTimeMillis() + 3_600_000)
        val alarm = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        ReminderNotificationScheduler.scheduleReminder(context, reminder)
        assertEquals(1, alarm.scheduledAlarms.size)
        context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE).edit().putBoolean("notifications_enabled", false).commit()
        ReminderNotificationScheduler.scheduleReminder(context, reminder)
        assertEquals(0, alarm.scheduledAlarms.size)
    }
    @Test fun `JSON export retains timestamps recurrence and completion data`() {
        val reminder = ReminderItem(id = 1, title = "Read", dueDateMillis = 1234567L, recurrence = "WEEKLY", isCompleted = true, completedAtMillis = 7654321L)
        val json = JSONArray(ReminderExport.create(listOf(reminder), "JSON").content).getJSONObject(0)
        assertEquals(1234567L, json.getLong("dueDateMillis"))
        assertEquals(7654321L, json.getLong("completedAtMillis"))
        assertEquals("WEEKLY", json.getString("recurrence"))
    }
    @Test fun `calendar export escapes text and folds UTF8 lines`() {
        val item = ReminderItem(id = 1, title = "Review, notes; and\\files", notes = "é".repeat(100) + "\nSecond line", recurrence = "DAILY")
        val calendar = ReminderExport.create(listOf(item), ".ICS").content
        assertTrue(calendar.contains("DUE:"))
        assertTrue(calendar.contains("SUMMARY:Review\\, notes\\; and\\\\files"))
        assertTrue(calendar.contains("RRULE:FREQ=DAILY"))
        assertTrue(calendar.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
    }
    @Test fun `voice errors give a real actionable failure`() {
        assertTrue(ReminderVoiceInput.errorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS).contains("microphone"))
        assertTrue(ReminderVoiceInput.errorMessage(SpeechRecognizer.ERROR_NETWORK).contains("connection"))
    }
    @Test fun `local endpoint detection accepts private addresses and rejects lookalike domains`() {
        assertTrue(LlmReminderClient.isLocalEndpoint("Custom", "https://192.168.1.8/v1/chat/completions"))
        assertFalse(LlmReminderClient.isLocalEndpoint("Custom", "https://10.example.com/v1/chat/completions"))
        assertFalse(LlmReminderClient.isLocalEndpoint("Custom", "https://localhost.example.com/v1/chat/completions"))
    }
}
