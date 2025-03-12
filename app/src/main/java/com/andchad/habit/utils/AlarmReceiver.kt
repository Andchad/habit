package com.andchad.habit.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat.startActivity
import com.andchad.habit.ui.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmUtils: AlarmUtils

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received")

        // Get the extras from the intent
        val habitId = intent.getStringExtra(AlarmUtils.KEY_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(AlarmUtils.KEY_HABIT_NAME) ?: "Habit Reminder"
        val vibrationEnabled = intent.getBooleanExtra(AlarmUtils.KEY_VIBRATION_ENABLED, false)
        val snoozeEnabled = intent.getBooleanExtra(AlarmUtils.KEY_SNOOZE_ENABLED, false)

        // Launch the alarm activity
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AlarmUtils.KEY_HABIT_ID, habitId)
            putExtra(AlarmUtils.KEY_HABIT_NAME, habitName)
            putExtra(AlarmUtils.KEY_VIBRATION_ENABLED, vibrationEnabled)
            putExtra(AlarmUtils.KEY_SNOOZE_ENABLED, snoozeEnabled)
        }

        context.startActivity(alarmIntent)

        // Vibrate if enabled - keep this functionality here
        if (vibrationEnabled) {
            vibrate(context)
        }

        // NOTE: Removed playAlarmSound method call from here
        // Let AlarmActivity handle playing and stopping the sound
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
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating device: ${e.message}")
        }
    }
}