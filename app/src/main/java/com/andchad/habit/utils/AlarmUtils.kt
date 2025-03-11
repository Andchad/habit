package com.andchad.habit.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmUtils @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AlarmUtils"

        // Alarm settings keys
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_NAME = "habit_name"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_SNOOZE_ENABLED = "snooze_enabled"

        // Snooze settings
        const val SNOOZE_TIME_MINUTES = 5
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(
        habitId: String,
        habitName: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean,
        snoozeEnabled: Boolean
    ) {
        // Cancel any existing alarms for this habit
        cancelAlarm(habitId)

        // If no days are selected, don't schedule anything
        if (scheduledDays.isEmpty()) {
            return
        }

        // Parse the reminder time
        val time = LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm"))

        // Find the next occurrence of any of the scheduled days
        val nextAlarmDateTime = findNextAlarmDate(time, scheduledDays)

        // Create the intent for the alarm
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(KEY_HABIT_ID, habitId)
            putExtra(KEY_HABIT_NAME, habitName)
            putExtra(KEY_VIBRATION_ENABLED, vibrationEnabled)
            putExtra(KEY_SNOOZE_ENABLED, snoozeEnabled)
        }

        // Create a unique request code for this habit
        val requestCode = habitId.hashCode()

        // Create the pending intent
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get the alarm time in milliseconds
        val alarmTimeMillis = nextAlarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for $habitName at $nextAlarmDateTime")
            } else {
                // Fall back to inexact alarm if permission not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled inexact alarm for $habitName at $nextAlarmDateTime")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTimeMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled exact alarm for $habitName at $nextAlarmDateTime")
        }
    }

    fun cancelAlarm(habitId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = habitId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for habit ID: $habitId")
        }
    }

    fun scheduleSnoozeAlarm(
        habitId: String,
        habitName: String,
        vibrationEnabled: Boolean
    ) {
        // Create the intent for the snoozed alarm
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(KEY_HABIT_ID, habitId)
            putExtra(KEY_HABIT_NAME, habitName)
            putExtra(KEY_VIBRATION_ENABLED, vibrationEnabled)
            putExtra(KEY_SNOOZE_ENABLED, false) // Prevent infinite snoozing
        }

        // Create a unique request code for this snoozed alarm
        val requestCode = (habitId + "_snooze").hashCode()

        // Create the pending intent
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate snooze time (current time + snooze duration)
        val snoozeTimeMillis = System.currentTimeMillis() + (SNOOZE_TIME_MINUTES * 60 * 1000)

        // Schedule the snoozed alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTimeMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTimeMillis,
                pendingIntent
            )
        }

        Log.d(TAG, "Scheduled snooze alarm for $habitName in $SNOOZE_TIME_MINUTES minutes")
    }

    private fun findNextAlarmDate(time: LocalTime, scheduledDays: List<DayOfWeek>): LocalDateTime {
        val now = LocalDateTime.now()
        val sortedDays = scheduledDays.sortedBy { it.value }

        // Check if today is one of the scheduled days and time hasn't passed yet
        if (sortedDays.contains(now.dayOfWeek)) {
            val todayDateTime = LocalDateTime.of(now.toLocalDate(), time)
            if (todayDateTime.isAfter(now)) {
                return todayDateTime
            }
        }

        // Find the next scheduled day
        var nextDate = now.toLocalDate()

        // Check the next 7 days
        for (i in 1..7) {
            nextDate = nextDate.plusDays(1)
            if (sortedDays.contains(nextDate.dayOfWeek)) {
                return LocalDateTime.of(nextDate, time)
            }
        }

        // If we couldn't find a matching day in the next week, use the first scheduled day of next week
        val firstScheduledDay = sortedDays.first()
        val nextOccurrence = now.toLocalDate().with(TemporalAdjusters.next(firstScheduledDay))
        return LocalDateTime.of(nextOccurrence, time)
    }
}