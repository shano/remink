package com.remink

import com.remink.data.model.ReminderValidation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for reminder field validation rules.
 * No mocks, no coroutines — all cases are synchronous.
 *
 * Assumes a top-level object/class `ReminderValidation` in `com.remink.data.model`
 * with the following API:
 *   fun isMessageValid(text: String): Boolean
 *   fun isScheduledTimeValid(scheduledAtMs: Long, nowMs: Long): Boolean
 */
class ReminderValidationTest {

    // --- Message validation ---

    @Test
    fun text_blank_isInvalid() {
        assertFalse(ReminderValidation.isMessageValid(""))
    }

    @Test
    fun text_whitespaceOnly_isInvalid() {
        assertFalse(ReminderValidation.isMessageValid("   "))
    }

    @Test
    fun text_over200chars_isInvalid() {
        val text = "a".repeat(201)
        assertFalse(ReminderValidation.isMessageValid(text))
    }

    @Test
    fun text_exactly200chars_isValid() {
        val text = "a".repeat(200)
        assertTrue(ReminderValidation.isMessageValid(text))
    }

    @Test
    fun text_oneChar_isValid() {
        assertTrue(ReminderValidation.isMessageValid("x"))
    }

    // --- Scheduled time validation ---

    @Test
    fun scheduledTime_inPast_isInvalid() {
        val nowMs = 1_000_000L
        val pastMs = nowMs - 1L
        assertFalse(ReminderValidation.isScheduledTimeValid(scheduledAtMs = pastMs, nowMs = nowMs))
    }

    @Test
    fun scheduledTime_inFuture_isValid() {
        val nowMs = 1_000_000L
        val futureMs = nowMs + 1L
        assertTrue(ReminderValidation.isScheduledTimeValid(scheduledAtMs = futureMs, nowMs = nowMs))
    }

    @Test
    fun scheduledTime_exactlyNow_isInvalid() {
        val nowMs = 1_000_000L
        assertFalse(ReminderValidation.isScheduledTimeValid(scheduledAtMs = nowMs, nowMs = nowMs))
    }
}
