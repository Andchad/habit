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
import com.andchad.habit.R
import com.andchad.habit.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object NotificationUtils {
    const val CHANNEL_ID = "habit_reminders"
    private const val CHANNEL_NAME = "Habit Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for your daily habits"

    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleHabitReminder(
        context: Context,
        habitId: String,
        habitName: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>
    ) {
        // Cancel any existing reminders for this habit
        cancelHabitReminder(context, habitId)

        // Parse the reminder time
        val time = LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
        val now = LocalDateTime.now()

        // If no days are selected, don't schedule anything
        if (scheduledDays.isEmpty()) {
            return
        }

        // Find the next occurrence of any of the scheduled days
        val nextReminderDate = findNextScheduledDate(now.toLocalDate(), time, scheduledDays)

        // Prepare the input data
        val inputData = Data.Builder()
            .putString("habitId", habitId)
            .putString("habitName", habitName)
            .putString("reminderTime", reminderTime)
            .putStringArray("scheduledDays", scheduledDays.map { it.name }.toTypedArray())
            .build()

        // Schedule the work
        scheduleReminderWork(context, habitId, nextReminderDate, inputData)
    }

    private fun findNextScheduledDate(
        fromDate: LocalDate,
        time: LocalTime,
        scheduledDays: List<DayOfWeek>
    ): LocalDateTime {
        val now = LocalDateTime.now()
        val sortedDays = scheduledDays.sortedBy { it.value }

        // Check if today is one of the scheduled days and time hasn't passed yet
        if (sortedDays.contains(fromDate.dayOfWeek)) {
            val todayDateTime = LocalDateTime.of(fromDate, time)
            if (todayDateTime.isAfter(now)) {
                return todayDateTime
            }
        }

        // Find the next scheduled day
        for (i in 1..7) { // Check the next 7 days
            val nextDate = fromDate.plusDays(i.toLong())
            if (sortedDays.contains(nextDate.dayOfWeek)) {
                return LocalDateTime.of(nextDate, time)
            }
        }

        // If we couldn't find a matching day in the next week, use the first scheduled day in the next week
        val firstDayOfWeek = sortedDays.first()
        val nextOccurrence = fromDate.with(TemporalAdjusters.next(firstDayOfWeek))
        return LocalDateTime.of(nextOccurrence, time)
    }

    private fun scheduleReminderWork(
        context: Context,
        habitId: String,
        dateTime: LocalDateTime,
        inputData: Data
    ) {
        val now = LocalDateTime.now()
        val delay = ChronoUnit.MILLIS.between(now, dateTime)

        if (delay <= 0) {
            // The time has already passed today, schedule for next occurrence
            return
        }

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

@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val habitId = inputData.getString("habitId") ?: return Result.failure()
        val habitName = inputData.getString("habitName") ?: return Result.failure()
        val reminderTime = inputData.getString("reminderTime") ?: return Result.failure()
        val scheduledDaysArray = inputData.getStringArray("scheduledDays")

        showNotification(habitId, habitName)

        // Schedule the next reminder
        if (scheduledDaysArray != null && scheduledDaysArray.isNotEmpty()) {
            val scheduledDays = scheduledDaysArray.mapNotNull { dayName ->
                try {
                    DayOfWeek.valueOf(dayName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            if (scheduledDays.isNotEmpty()) {
                NotificationUtils.scheduleHabitReminder(
                    context,
                    habitId,
                    habitName,
                    reminderTime,
                    scheduledDays
                )
            }
        }

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