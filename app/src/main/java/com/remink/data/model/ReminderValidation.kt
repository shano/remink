package com.remink.data.model

object ReminderValidation {

    fun isMessageValid(text: String): Boolean =
        text.isNotBlank() && text.length <= 200

    fun isScheduledTimeValid(scheduledAtMs: Long, nowMs: Long): Boolean =
        scheduledAtMs > nowMs
}
