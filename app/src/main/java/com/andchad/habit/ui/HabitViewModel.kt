package com.andchad.habit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andchad.habit.data.HabitRepository
import com.andchad.habit.data.model.Habit
import com.andchad.habit.utils.AlarmUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val alarmUtils: AlarmUtils,
    application: Application
) : AndroidViewModel(application) {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Store custom sort order
    private val _habitOrder = MutableStateFlow<List<String>>(emptyList())

    init {
        // Observe habits from repository
        viewModelScope.launch {
            habitRepository.getHabits().collect { habitsList ->
                if (_habitOrder.value.isEmpty()) {
                    // Initialize habit order if empty
                    _habitOrder.value = habitsList.map { it.id }
                } else {
                    // Update order with any new habits
                    val currentOrder = _habitOrder.value.toMutableList()
                    habitsList.forEach { habit ->
                        if (!currentOrder.contains(habit.id)) {
                            currentOrder.add(habit.id)
                        }
                    }
                    // Remove IDs of deleted habits
                    val existingIds = habitsList.map { it.id }
                    _habitOrder.value = currentOrder.filter { existingIds.contains(it) }
                }

                // Sort habits according to user order
                val sortedHabits = habitsList.sortedBy { habit ->
                    _habitOrder.value.indexOf(habit.id).let {
                        if (it >= 0) it else Int.MAX_VALUE
                    }
                }
                _habits.value = sortedHabits
            }
        }
    }

    // Check if a habit with this name already exists
    suspend fun habitNameExists(name: String): Boolean {
        return habitRepository.habitWithNameExists(name)
    }

    fun createHabit(
        name: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean,
        snoozeEnabled: Boolean
    ) {
        viewModelScope.launch {
            val habit = habitRepository.createHabit(
                name,
                reminderTime,
                scheduledDays,
                vibrationEnabled,
                snoozeEnabled
            )

            // Schedule alarm
            scheduleHabitAlarm(habit)
        }
    }

    fun updateHabit(
        id: String,
        name: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean,
        snoozeEnabled: Boolean
    ) {
        viewModelScope.launch {
            habitRepository.updateHabit(
                id,
                name,
                reminderTime,
                scheduledDays,
                vibrationEnabled,
                snoozeEnabled
            )

            // Update alarm
            val habit = _habits.value.find { it.id == id }
            if (habit != null) {
                // Cancel the existing alarm
                alarmUtils.cancelAlarm(habit.id)

                // Schedule with the new details
                scheduleHabitAlarm(habit.copy(
                    name = name,
                    reminderTime = reminderTime,
                    scheduledDays = scheduledDays,
                    vibrationEnabled = vibrationEnabled,
                    snoozeEnabled = snoozeEnabled
                ))
            }
        }
    }

    fun updateVibrationSetting(id: String, vibrationEnabled: Boolean) {
        viewModelScope.launch {
            habitRepository.updateVibrationSetting(id, vibrationEnabled)

            // Update alarm
            val habit = _habits.value.find { it.id == id } ?: return@launch
            alarmUtils.cancelAlarm(habit.id)
            scheduleHabitAlarm(habit.copy(vibrationEnabled = vibrationEnabled))
        }
    }

    fun updateSnoozeSetting(id: String, snoozeEnabled: Boolean) {
        viewModelScope.launch {
            habitRepository.updateSnoozeSetting(id, snoozeEnabled)

            // Update alarm
            val habit = _habits.value.find { it.id == id } ?: return@launch
            alarmUtils.cancelAlarm(habit.id)
            scheduleHabitAlarm(habit.copy(snoozeEnabled = snoozeEnabled))
        }
    }

    fun completeHabit(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            habitRepository.completeHabit(id, isCompleted)

            // If marked as completed, cancel the alarm
            if (isCompleted) {
                alarmUtils.cancelAlarm(id)
            } else {
                // Re-schedule alarm if marked as not completed
                val habit = _habits.value.find { it.id == id }
                if (habit != null) {
                    scheduleHabitAlarm(habit.copy(isCompleted = false))
                }
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)

            // Cancel alarm
            alarmUtils.cancelAlarm(habit.id)
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    private fun scheduleHabitAlarm(habit: Habit) {
        // Only schedule if habit is not completed
        if (!habit.isCompleted) {
            alarmUtils.scheduleAlarm(
                habitId = habit.id,
                habitName = habit.name,
                reminderTime = habit.reminderTime,
                scheduledDays = habit.scheduledDays,
                vibrationEnabled = habit.vibrationEnabled,
                snoozeEnabled = habit.snoozeEnabled
            )
        }
    }
}