package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ReminderItem
import com.example.data.nlp.NlpReminderParser
import com.example.notification.NotificationHelper
import com.example.notification.ReminderNotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FlowMind", appName)
  }

  @Test
  fun `test nlp parser extracts entities correctly`() {
    val input = "Call dentist on Monday at 9:00 AM with Dr. Aris urgent #health"
    val result = NlpReminderParser.parse(input)

    assertEquals("HIGH", result.priority)
    assertEquals("#health", result.tag)
    assertEquals("Health", result.category)
    assertEquals("Dr. Aris", result.participant)
    assertTrue(result.entities.isNotEmpty())
  }

  @Test
  fun `test nlp parser handles both dot and colon time formats and fullstops`() {
    // 1. Dot format with PM: 7.00pm
    val resultDotPm = NlpReminderParser.parse("remind me at 7.00pm")
    assertTrue(resultDotPm.dueTimeString.contains("7:00 PM"))
    assertEquals(false, resultDotPm.needsLlmAssistance)
    assertEquals("Keyword Classifier (Local)", resultDotPm.parsingEngine)

    // 2. Colon format with PM: 7:00pm
    val resultColonPm = NlpReminderParser.parse("remind me at 7:00pm")
    assertTrue(resultColonPm.dueTimeString.contains("7:00 PM"))
    assertEquals(false, resultColonPm.needsLlmAssistance)

    // 3. Dot format without AM/PM: 7.00
    val resultDotBare = NlpReminderParser.parse("remind me at 7.00")
    assertTrue(resultDotBare.dueTimeString.contains("7:00"))
    assertEquals(false, resultDotBare.needsLlmAssistance)

    // 4. Colon format without AM/PM: 7:00
    val resultColonBare = NlpReminderParser.parse("remind me at 7:00")
    assertTrue(resultColonBare.dueTimeString.contains("7:00"))
    assertEquals(false, resultColonBare.needsLlmAssistance)

    // 5. Ending with a fullstop: 7.00pm.
    val resultWithFullstop = NlpReminderParser.parse("remind me at 7.00pm.")
    assertTrue(resultWithFullstop.dueTimeString.contains("7:00 PM"))
    assertEquals(false, resultWithFullstop.needsLlmAssistance)
    assertTrue(!resultWithFullstop.cleanTitle.endsWith("."))

    // 6. Conversational "i need to be reminded at 7.00pm"
    val resultConversational = NlpReminderParser.parse("i need to be reminded at 7.00pm")
    assertTrue(resultConversational.dueTimeString.contains("7:00 PM"))
    assertEquals(false, resultConversational.needsLlmAssistance)
  }

  @Test
  fun `test notification channel initialization and scheduler`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    NotificationHelper.initNotificationChannels(context)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_REMINDERS_ID)
    assertNotNull(channel)
    assertEquals(NotificationHelper.CHANNEL_REMINDERS_ID, channel.id)

    val testReminder = ReminderItem(
      id = 999,
      title = "Quarterly Sync",
      notes = "Review revenue goals",
      category = "Work",
      tag = "#work",
      priority = "HIGH",
      dueDateMillis = System.currentTimeMillis() + 600000L,
      dueTimeString = "Today · 3:00 PM",
      isCompleted = false
    )

    // Verify scheduling does not crash
    ReminderNotificationScheduler.scheduleReminder(context, testReminder, 0)
    ReminderNotificationScheduler.cancelReminder(context, testReminder.id)
  }
}

