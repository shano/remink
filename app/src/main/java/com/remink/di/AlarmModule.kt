package com.remink.di

import android.app.AlarmManager
import android.content.Context
import com.remink.alarm.AlarmScheduler
import com.remink.alarm.AlarmSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlarmProviderModule {

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    @Singleton
    fun provideAlarmSchedulerImpl(
        @ApplicationContext context: Context,
        alarmManager: AlarmManager,
    ): AlarmSchedulerImpl = AlarmSchedulerImpl(context, alarmManager)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmBindingModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AlarmSchedulerImpl): AlarmScheduler
}
