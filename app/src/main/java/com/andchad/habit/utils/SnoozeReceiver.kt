package com.andchad.habit.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SnoozeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmUtils: AlarmUtils

    companion object {
        private const val TAG = "SnoozeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Snooze request received")

        // Extract habit details from intent
        val habitId = intent.getStringExtra(AlarmUtils.KEY_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(AlarmUtils.KEY_HABIT_NAME) ?: "Habit Reminder"
        val vibrationEnabled = intent.getBooleanExtra(AlarmUtils.KEY_VIBRATION_ENABLED, false)

        // Schedule a new alarm for the snooze duration
        alarmUtils.scheduleSnoozeAlarm(habitId, habitName, vibrationEnabled)

        Log.d(TAG, "Alarm snoozed for habit: $habitName")
    }
}