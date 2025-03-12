package com.andchad.habit

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.andchad.habit.utils.AdManager
import com.andchad.habit.utils.HabitHistoryWorker
import com.andchad.habit.utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HabitApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize notification channel
        NotificationUtils.createNotificationChannel(this)

        // Initialize Ad SDK
        adManager.initialize()

        // Schedule the habit history worker
        scheduleHabitHistoryJob()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    private fun scheduleHabitHistoryJob() {
        // Schedule for 11:59 PM
        val now = LocalDateTime.now()
        val todayEnd = LocalDateTime.of(
            now.toLocalDate(),
            LocalTime.of(23, 59, 0)
        )

        // If it's already past 11:59 PM, schedule for tomorrow
        var scheduledTime = todayEnd
        if (now.isAfter(todayEnd)) {
            scheduledTime = todayEnd.plusDays(1)
        }

        val initialDelay = Duration.between(now, scheduledTime).toMillis()

        // Daily work request
        val habitHistoryWorkRequest = PeriodicWorkRequestBuilder<HabitHistoryWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "habit_history_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            habitHistoryWorkRequest
        )
    }
}