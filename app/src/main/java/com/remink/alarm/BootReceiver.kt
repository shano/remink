package com.remink.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.remink.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: ReminderRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = repository.getFutureUnacknowledged()
                for (reminder in reminders) {
                    alarmScheduler.schedule(reminder.id, reminder.scheduledAt)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
