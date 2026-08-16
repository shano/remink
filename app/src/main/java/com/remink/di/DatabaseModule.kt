package com.remink.di

import android.content.Context
import androidx.room.Room
import com.remink.data.db.ReminkDatabase
import com.remink.data.db.ReminderDao
import com.remink.data.repository.ReminderRepository
import com.remink.data.repository.ReminderRepositoryImpl
import com.remink.util.Clock
import com.remink.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReminkDatabase =
        Room.databaseBuilder(context, ReminkDatabase::class.java, "remink.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideReminderDao(database: ReminkDatabase): ReminderDao = database.reminderDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
