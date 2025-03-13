package com.andchad.habit.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.andchad.habit.R
import com.andchad.habit.ui.AlarmActivity
import com.andchad.habit.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmUtils: AlarmUtils

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val NOTIFICATION_CHANNEL_ID = "habit_alarm_channel"
        private const val NOTIFICATION_ID = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver: Received intent with action: ${intent.action}")

        // Wake up the device if it's sleeping
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "habit:wakelock"
        )
        wakeLock.acquire(10*60*1000L /*10 minutes*/)

        // Get the extras from the intent
        val habitId = intent.getStringExtra(AlarmUtils.KEY_HABIT_ID)
        val habitName = intent.getStringExtra(AlarmUtils.KEY_HABIT_NAME) ?: "Habit Reminder"
        val vibrationEnabled = intent.getBooleanExtra(AlarmUtils.KEY_VIBRATION_ENABLED, false)
        val snoozeEnabled = intent.getBooleanExtra(AlarmUtils.KEY_SNOOZE_ENABLED, false)

        Log.d(TAG, "Alarm triggered for habit: $habitName (ID: $habitId)")

        // Ensure we have a valid habit ID
        if (habitId.isNullOrEmpty()) {
            Log.e(TAG, "Missing habit ID in alarm intent")
            wakeLock.release()
            return
        }

        // Create notification channel for Android O and above
        createNotificationChannel(context)

        // Show a notification first to ensure system is aware
        showNotification(context, habitId, habitName, snoozeEnabled)

        // Launch the alarm activity with high priority flags
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            // Add flags to show even if device is locked
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra(AlarmUtils.KEY_HABIT_ID, habitId)
            putExtra(AlarmUtils.KEY_HABIT_NAME, habitName)
            putExtra(AlarmUtils.KEY_VIBRATION_ENABLED, vibrationEnabled)
            putExtra(AlarmUtils.KEY_SNOOZE_ENABLED, snoozeEnabled)
        }

        try {
            context.startActivity(alarmIntent)
            Log.d(TAG, "Started AlarmActivity for habit: $habitName")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AlarmActivity: ${e.message}", e)
        }

        // Vibrate if enabled
        if (vibrationEnabled) {
            vibrate(context)
        }

        // Release the wake lock
        wakeLock.release()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for habit reminders"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, habitId: String, habitName: String, snoozeEnabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open the app when notification is tapped
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a full screen intent to show AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AlarmUtils.KEY_HABIT_ID, habitId)
            putExtra(AlarmUtils.KEY_HABIT_NAME, habitName)
            putExtra(AlarmUtils.KEY_VIBRATION_ENABLED, true)
            putExtra(AlarmUtils.KEY_SNOOZE_ENABLED, snoozeEnabled)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode() + 100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time for your habit!")
            .setContentText(habitName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)

        notificationManager.notify(NOTIFICATION_ID + habitId.hashCode(), builder.build())
    }

    private fun vibrate(context: Context) {
        try {
            val vibrationPattern = longArrayOf(0, 500, 500, 500, 500, 500)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator

                val effect = VibrationEffect.createWaveform(vibrationPattern, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(vibrationPattern, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationPattern, -1)
                }
            }
            Log.d(TAG, "Vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating device: ${e.message}", e)
        }
    }
}