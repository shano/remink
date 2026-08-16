package com.remink.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

interface AlarmScheduler {
    fun schedule(reminderId: Long, scheduledAt: Long)
    fun cancel(reminderId: Long)
}

class AlarmSchedulerImpl(
    private val context: Context,
    private val alarmManager: AlarmManager,
) : AlarmScheduler {

    override fun schedule(reminderId: Long, scheduledAt: Long) {
        val alarmPendingIntent = buildAlarmPendingIntent(reminderId)
        // showIntent opens the main app when user taps the system clock indicator
        val showIntent = Intent().apply {
            setClassName(context.packageName, "${context.packageName}.MainActivity")
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(scheduledAt, showPendingIntent)
        alarmManager.setAlarmClock(info, alarmPendingIntent)
    }

    override fun cancel(reminderId: Long) {
        val pendingIntent = buildAlarmPendingIntent(reminderId)
        alarmManager.cancel(pendingIntent)
    }

    private fun buildAlarmPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(ACTION_ALARM).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
