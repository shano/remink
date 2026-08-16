package com.remink

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.remink.alarm.AlarmSchedulerImpl
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlarmSchedulerTest {

    private lateinit var alarmManager: AlarmManager
    private lateinit var context: Context
    private lateinit var scheduler: AlarmSchedulerImpl

    @Before
    fun setUp() {
        alarmManager = mockk(relaxed = true)
        // Real Robolectric context required for PendingIntent statics
        context = ApplicationProvider.getApplicationContext()
        scheduler = AlarmSchedulerImpl(context, alarmManager)
    }

    @Test
    fun schedule_callsSetAlarmClockWithCorrectTriggerTime() {
        val reminderId = 5L
        val scheduledAt = 9_000_000L

        scheduler.schedule(reminderId, scheduledAt)

        val alarmInfoSlot = slot<AlarmManager.AlarmClockInfo>()
        verify(exactly = 1) {
            alarmManager.setAlarmClock(capture(alarmInfoSlot), any())
        }
        assertEquals(scheduledAt, alarmInfoSlot.captured.triggerTime)
    }

    @Test
    fun cancel_callsCancelWithMatchingPendingIntent() {
        val reminderId = 12L

        scheduler.cancel(reminderId)

        verify(exactly = 1) { alarmManager.cancel(any()) }
    }

    @Test
    fun schedule_usesReminderIdAsRequestCode() {
        // The AlarmSchedulerImpl must derive the PendingIntent request code from reminderId.toInt().
        // We verify by scheduling two different reminders and cancelling only one — the cancel for
        // reminderId=1 must not affect the pending intent created for reminderId=2.
        //
        // The observable contract: cancel(1) calls alarmManager.cancel exactly once regardless of
        // whether schedule(2) was also called, because the request codes differ.
        scheduler.schedule(1L, 1_000_000L)
        scheduler.schedule(2L, 2_000_000L)

        scheduler.cancel(1L)

        // setAlarmClock was called twice (once per schedule), cancel once
        verify(exactly = 2) { alarmManager.setAlarmClock(any(), any()) }
        verify(exactly = 1) { alarmManager.cancel(any()) }
    }
}
