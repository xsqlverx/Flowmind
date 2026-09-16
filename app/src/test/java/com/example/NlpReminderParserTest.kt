package com.example

import com.example.data.nlp.NlpReminderParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NlpReminderParserTest {

    @Test
    fun testWorkReminderParsing() {
        val input = "Schedule team sprint planning tomorrow at 10am with Alex high priority"
        val result = NlpReminderParser.parse(input)

        assertEquals("Work", result.category)
        assertEquals("#work", result.tag)
        assertEquals("HIGH", result.priority)
        assertEquals("Alex", result.participant)
        assertFalse("Standard work reminder should be handled locally", result.needsLlmAssistance)
        assertTrue(result.parsingEngine.contains("Keyword Classifier"))
    }

    @Test
    fun testHealthAndRecurrenceParsing() {
        val input = "Gym workout every weekday at 7am"
        val result = NlpReminderParser.parse(input)

        assertEquals("Health", result.category)
        assertEquals("WEEKDAYS", result.recurrence)
        assertFalse(result.needsLlmAssistance)
    }

    @Test
    fun testFinanceReminderParsing() {
        val input = "Pay credit card bill this Friday urgent"
        val result = NlpReminderParser.parse(input)

        assertEquals("Finance", result.category)
        assertEquals("HIGH", result.priority)
        assertFalse(result.needsLlmAssistance)
    }

    @Test
    fun testExplicitAiPrefixTriggersLlm() {
        val input = "ai: analyze my messy draft and set a reminder if needed"
        val result = NlpReminderParser.parse(input)

        assertTrue("Explicit AI trigger must flag for LLM assistance", result.needsLlmAssistance)
        assertTrue(result.parsingEngine.contains("Neural Assistant"))
    }

    @Test
    fun testTwentyFourOutOfTwentyFiveHandledLocally() {
        val sampleReminders = listOf(
            "Call dentist at 2pm tomorrow",
            "Pick up dry cleaning on Thursday afternoon",
            "Send invoice to Sarah urgent #finance",
            "Review pull request #402 with David tonight",
            "Buy groceries tonight at 6pm",
            "Daily team standup at 9:30am repeat daily",
            "Submit tax documents by Friday",
            "Water the plants tomorrow morning",
            "Pay electric bill on the 1st of next month",
            "Team retrospective meeting next Wednesday with Elena",
            "Feed the dog at 8pm",
            "Study Kotlin coroutines for 1 hour tonight",
            "Take vitamins every morning at 8am",
            "Renew car insurance next Monday high priority",
            "Coffee catch up with Marcus at 3pm",
            "Deploy release candidate v2.4 tonight at 11pm",
            "Clean the kitchen before guests arrive tomorrow",
            "Dentist checkup next Tuesday 10am",
            "Cancel subscription trial before Friday",
            "Write weekly status report with Michael",
            "Refill prescription medication tomorrow",
            "Prepare slides for marketing demo on Thursday",
            "Pay rent on Monday morning urgent",
            "Check server memory logs at midnight",
            // The 1 complex / conversational edge case:
            "ai: figure out when I should remind myself considering all conflicting schedules"
        )

        var localCount = 0
        var llmCount = 0

        for (reminder in sampleReminders) {
            val res = NlpReminderParser.parse(reminder)
            if (!res.needsLlmAssistance) {
                localCount++
            } else {
                llmCount++
            }
        }

        assertTrue("At least 24 out of 25 reminders must be caught by local keyword classifier", localCount >= 24)
        assertEquals(1, llmCount)
    }
}
