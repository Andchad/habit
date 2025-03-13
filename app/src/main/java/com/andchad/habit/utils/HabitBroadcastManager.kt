package com.andchad.habit.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Manages local broadcasts for communication between different components of the app.
 * This is used primarily to notify the MainActivity when alarm actions occur.
 */
object HabitBroadcastManager {
    private const val TAG = "HabitBroadcastManager"

    // Broadcast actions
    const val ACTION_ALARM_DISMISSED = "com.andchad.habit.ACTION_ALARM_DISMISSED"
    const val ACTION_ALARM_COMPLETED = "com.andchad.habit.ACTION_ALARM_COMPLETED"
    const val ACTION_HABIT_UPDATE = "com.andchad.habit.ACTION_HABIT_UPDATE"

    // Extra keys
    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_HABIT_NAME = "habit_name"
    const val EXTRA_ACTION_TYPE = "action_type"

    // Action types
    const val ACTION_TYPE_COMPLETED = "completed"
    const val ACTION_TYPE_DISMISSED = "dismissed"

    /**
     * Send a broadcast when an alarm is dismissed
     */
    fun sendAlarmDismissedBroadcast(context: Context, habitId: String, habitName: String) {
        val intent = Intent(ACTION_ALARM_DISMISSED).apply {
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
        }
        Log.d(TAG, "Sending alarm dismissed broadcast for habit: $habitName")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Send a broadcast when a habit is completed from the alarm screen
     */
    fun sendAlarmCompletedBroadcast(context: Context, habitId: String, habitName: String) {
        val intent = Intent(ACTION_ALARM_COMPLETED).apply {
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
        }
        Log.d(TAG, "Sending alarm completed broadcast for habit: $habitName")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Send a general habit update broadcast
     */
    fun sendHabitUpdateBroadcast(context: Context, habitId: String, actionType: String) {
        val intent = Intent(ACTION_HABIT_UPDATE).apply {
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_ACTION_TYPE, actionType)
        }
        Log.d(TAG, "Sending habit update broadcast: $actionType for habit $habitId")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Register a receiver for alarm-related broadcasts
     */
    fun registerReceiver(context: Context, receiver: BroadcastReceiver) {
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_ALARM_DISMISSED)
            addAction(ACTION_ALARM_COMPLETED)
            addAction(ACTION_HABIT_UPDATE)
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter)
        Log.d(TAG, "Registered broadcast receiver")
    }

    /**
     * Unregister a receiver
     */
    fun unregisterReceiver(context: Context, receiver: BroadcastReceiver) {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
            Log.d(TAG, "Unregistered broadcast receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
}