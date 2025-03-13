package com.andchad.habit.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andchad.habit.data.HabitRepository
import com.andchad.habit.data.model.Habit
import com.andchad.habit.data.model.HabitHistory
import com.andchad.habit.data.model.HabitHistoryFactory
import com.andchad.habit.data.model.HabitStatus
import com.andchad.habit.utils.AlarmUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val alarmUtils: AlarmUtils
) : ViewModel() {

    private val TAG = "HabitViewModel"

    // State flows for UI state
    private val _habitCreationStatus = MutableStateFlow<HabitCreationStatus>(HabitCreationStatus.Initial)
    val habitCreationStatus: StateFlow<HabitCreationStatus> = _habitCreationStatus

    // Dark mode state - keep this to match original ViewModel
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    // Show today's habits only - keep this to match original ViewModel
    private val _showTodayHabitsOnly = MutableStateFlow(false)
    val showTodayHabitsOnly: StateFlow<Boolean> = _showTodayHabitsOnly

    // Get all habits as a StateFlow
    val habits = repository.getHabits().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // For compatibility with original ViewModel
    val allHabits = habits

    // Get all habit history as a StateFlow
    val habitHistory = repository.getHabitHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Combined flow for the habit name map (for displaying names in history view)
    val habitNameMap = habits.combine(habitHistory) { habitsList, _ ->
        habitsList.associate { it.id to it.name }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

    // Function to toggle between today's habits and all habits
    fun toggleHabitsFilter() {
        _showTodayHabitsOnly.value = !_showTodayHabitsOnly.value
    }

    // Function to toggle dark mode
    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Check if a habit is upcoming (for UI display) - from original ViewModel
    fun isHabitUpcoming(habit: Habit): Boolean {
        try {
            // Get the current date and day of week
            val today = LocalDate.now()
            val currentDayOfWeek = today.dayOfWeek
            val now = LocalTime.now()

            // Check if the habit is scheduled for a future day
            if (habit.scheduledDays.any { it != currentDayOfWeek && isDateAfterToday(today, it) }) {
                return true
            }

            // If scheduled for today, check if the time is in the future
            if (habit.scheduledDays.contains(currentDayOfWeek)) {
                val habitTime = habit.getReminderTimeAsLocalTime()
                return habitTime.isAfter(now)
            }

            // If not scheduled for today or any future day, it's not upcoming
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if habit is upcoming: ${e.message}")
            return false
        }
    }

    // Helper method to determine if a day is after today
    private fun isDateAfterToday(today: LocalDate, dayOfWeek: DayOfWeek): Boolean {
        val daysUntilNext = (dayOfWeek.value - today.dayOfWeek.value + 7) % 7
        return daysUntilNext > 0
    }

    // Force refresh habits - for compatibility with original ViewModel
    fun forceRefreshHabits() {
        // No-op: Flow will automatically update when database changes
    }

    // Create a new habit
    fun createHabit(
        name: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean = true,
        snoozeEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                // Check if habit with the same name exists
                if (repository.habitWithNameExists(name)) {
                    _habitCreationStatus.value = HabitCreationStatus.NameExists
                    return@launch
                }

                // Create the habit
                val habit = repository.createHabit(
                    name,
                    reminderTime,
                    scheduledDays,
                    vibrationEnabled,
                    snoozeEnabled
                )

                // Schedule the alarm for this habit
                scheduleHabitAlarm(habit)

                _habitCreationStatus.value = HabitCreationStatus.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error creating habit: $e")
                _habitCreationStatus.value = HabitCreationStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Update existing habit
    fun updateHabit(
        id: String,
        name: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean,
        snoozeEnabled: Boolean
    ) {
        viewModelScope.launch {
            try {
                repository.updateHabit(
                    id,
                    name,
                    reminderTime,
                    scheduledDays,
                    vibrationEnabled,
                    snoozeEnabled
                )

                // Cancel and reschedule the alarm with updated settings
                val habit = habits.value.find { it.id == id }
                if (habit != null) {
                    // First cancel any existing alarm
                    alarmUtils.cancelAlarm(id)

                    // Create a copy with the updated values for scheduling
                    val updatedHabit = habit.copy(
                        name = name,
                        reminderTime = reminderTime,
                        scheduledDays = scheduledDays,
                        vibrationEnabled = vibrationEnabled,
                        snoozeEnabled = snoozeEnabled
                    )

                    // Reschedule the alarm
                    scheduleHabitAlarm(updatedHabit)
                }

                _habitCreationStatus.value = HabitCreationStatus.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error updating habit: $e")
                _habitCreationStatus.value = HabitCreationStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Delete a habit
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                // Delete the habit and its history
                repository.deleteHabitWithHistory(habit)

                // Cancel any scheduled alarms for this habit
                alarmUtils.cancelAlarm(habit.id)

                Log.d(TAG, "Habit deleted: ${habit.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting habit: $e")
            }
        }
    }

    // Delete all habits
    fun deleteAllHabits() {
        viewModelScope.launch {
            try {
                // Get all habits first
                val allHabits = habits.value

                // Delete each habit one by one to ensure all alarms are cancelled
                allHabits.forEach { habit ->
                    repository.deleteHabitWithHistory(habit)
                    alarmUtils.cancelAlarm(habit.id)
                }

                Log.d(TAG, "All habits deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all habits: $e")
            }
        }
    }

    // Mark habit as completed
    fun completeHabit(habitId: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                // Update the habit completion status
                repository.completeHabit(habitId, completed)

                // Create a history entry for this completion
                val history = HabitHistoryFactory.createForToday(habitId, true)
                repository.saveHabitHistory(history)

                Log.d(TAG, "Habit marked completed: $habitId, $completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error completing habit: $e")
            }
        }
    }

    // Dismiss the habit for today (mark as missed)
    fun dismissHabit(habitId: String) {
        viewModelScope.launch {
            try {
                // Create a history entry marked as MISSED
                val history = HabitHistoryFactory.create(habitId, HabitStatus.MISSED)
                repository.saveHabitHistory(history)

                // Also update the habit completion status to true (to remove from UI)
                repository.completeHabit(habitId, true)

                Log.d(TAG, "Habit dismissed for today: $habitId")
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing habit: $e")
            }
        }
    }

    // Clear all habit history
    fun clearAllHabitHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllHabitHistory()
                Log.d(TAG, "All habit history cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing habit history: $e")
            }
        }
    }

    // Check if a habit name exists (for validation)
    suspend fun habitNameExists(name: String): Boolean {
        // Check if any existing habit has this name (case-insensitive)
        return repository.habitWithNameExists(name)
    }

    // Schedule alarm for a habit
    private fun scheduleHabitAlarm(habit: Habit) {
        if (habit.scheduledDays.isEmpty()) {
            // No need to schedule if no days are selected
            Log.d(TAG, "Not scheduling alarm for ${habit.name} - no days selected")
            return
        }

        // Schedule the alarm
        alarmUtils.scheduleAlarm(
            habitId = habit.id,
            habitName = habit.name,
            reminderTime = habit.reminderTime,
            scheduledDays = habit.scheduledDays,
            vibrationEnabled = habit.vibrationEnabled,
            snoozeEnabled = habit.snoozeEnabled
        )

        Log.d(TAG, "Scheduled alarm for habit: ${habit.name}")
    }

    // Reset habit creation status
    fun resetHabitCreationStatus() {
        _habitCreationStatus.value = HabitCreationStatus.Initial
    }

    // Habit creation status states
    sealed class HabitCreationStatus {
        object Initial : HabitCreationStatus()
        object Success : HabitCreationStatus()
        object NameExists : HabitCreationStatus()
        data class Error(val message: String) : HabitCreationStatus()
    }
}