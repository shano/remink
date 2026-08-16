package com.remink.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.remink.data.model.Reminder

@Database(
    entities = [Reminder::class],
    version = 1,
    exportSchema = false,
)
abstract class ReminkDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
}
