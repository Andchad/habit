package com.andchad.habit.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.andchad.habit.R
import com.andchad.habit.ui.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object NotificationUtils {
    const val CHANNEL_ID = "habit_reminders"
    private const val CHANNEL_NAME = "Habit Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for your daily habits"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleHabitReminder(
        context: Context,
        habitId: String,
        habitName: String,
        reminderTime: String
    ) {
        val time = LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
        val now = LocalDateTime.now()
        val targetDateTime = LocalDateTime.of(
            LocalDate.now(),
            time
        )

        // If the time for today has already passed, schedule for tomorrow
        val finalTargetDateTime = if (targetDateTime.isBefore(now)) {
            targetDateTime.plusDays(1)
        } else {
            targetDateTime
        }

        val delay = finalTargetDateTime.toInstant(ZoneOffset.UTC).toEpochMilli() -
                now.toInstant(ZoneOffset.UTC).toEpochMilli()

        val inputData = Data.Builder()
            .putString("habitId", habitId)
            .putString("habitName", habitName)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val notificationWork = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "habit_reminder_$habitId",
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )
    }

    fun cancelHabitReminder(context: Context, habitId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("habit_reminder_$habitId")
    }
}

class HabitReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val habitId = inputData.getString("habitId") ?: return Result.failure()
        val habitName = inputData.getString("habitName") ?: return Result.failure()

        showNotification(habitId, habitName)

        // Schedule the next reminder for tomorrow
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        NotificationUtils.scheduleHabitReminder(
            context,
            habitId,
            habitName,
            time
        )

        return Result.success()
    }

    private fun showNotification(habitId: String, habitName: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("habitId", habitId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Habit Reminder")
            .setContentText("It's time for your habit: $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // Check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(habitId.hashCode(), builder.build())
            }
            // If permission is not granted, we simply don't show the notification
            // In a real app, you might want to log this or have an alternative
        } else {
            // For Android versions below 13, permission is granted at install time
            notificationManager.notify(habitId.hashCode(), builder.build())
        }
    }
}