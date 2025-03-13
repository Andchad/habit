package com.andchad.habit.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andchad.habit.data.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var alarmUtils: AlarmUtils

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device rebooted, restoring alarms")

            // Restore all alarms
            scope.launch {
                try {
                    habitRepository.getHabits().collect { habits ->
                        // Only restore alarms for uncompleted habits
                        val activeHabits = habits.filter { !it.isCompleted }

                        for (habit in activeHabits) {
                            alarmUtils.scheduleAlarm(
                                habitId = habit.id,
                                habitName = habit.name,
                                reminderTime = habit.reminderTime,
                                scheduledDays = habit.scheduledDays,
                                vibrationEnabled = habit.vibrationEnabled,
                                snoozeEnabled = habit.snoozeEnabled
                            )
                            Log.d(TAG, "Restored alarm for habit: ${habit.name}")
                        }

                        Log.d(TAG, "Restored ${activeHabits.size} alarms")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring alarms: ${e.message}", e)
                }
            }
        }
    }
}