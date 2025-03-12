package com.andchad.habit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andchad.habit.data.HabitRepository
import com.andchad.habit.data.model.Habit
import com.andchad.habit.data.model.HabitHistory
import com.andchad.habit.data.model.HabitStatus
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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

    // Add state for showing today's habits only
    private val _showTodayHabitsOnly = MutableStateFlow(false)
    val showTodayHabitsOnly: StateFlow<Boolean> = _showTodayHabitsOnly.asStateFlow()

    // Store custom sort order
    private val _habitOrder = MutableStateFlow<List<String>>(emptyList())

    // Store all unfiltered habits
    private val _allHabits = MutableStateFlow<List<Habit>>(emptyList())

    // Add state for habit history
    private val _habitHistory = MutableStateFlow<List<HabitHistory>>(emptyList())
    val habitHistory: StateFlow<List<HabitHistory>> = _habitHistory.asStateFlow()

    // Map of habit IDs to their names for displaying in history
    private val _habitNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val habitNameMap: StateFlow<Map<String, String>> = _habitNameMap.asStateFlow()

    init {
        // Observe habits from repository
        viewModelScope.launch {
            habitRepository.getHabits().collect { habitsList ->
                // Store all habits
                _allHabits.value = habitsList

                // Apply order and filtering
                applyHabitsFilter()

                // Update the habit name map
                val newMap = habitsList.associate { it.id to it.name }
                _habitNameMap.value = newMap
            }
        }

        // Reapply filter when the filter mode changes
        viewModelScope.launch {
            _showTodayHabitsOnly.collect {
                applyHabitsFilter()
            }
        }

        // Fetch history data
        viewModelScope.launch {
            try {
                habitRepository.getHabitHistory().collect { historyFromRepo ->
                    _habitHistory.value = historyFromRepo
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitViewModel", "Error fetching habit history: ${e.message}")
            }
        }
    }

    // Function to toggle between today's habits and all habits
    fun toggleHabitsFilter() {
        _showTodayHabitsOnly.value = !_showTodayHabitsOnly.value
    }

    // Apply filtering and sorting to habits
    private fun applyHabitsFilter() {
        val habitsList = _allHabits.value

        // Update order with any new habits
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

        // Filter for today if needed
        val filteredList = if (_showTodayHabitsOnly.value) {
            val today = LocalDate.now().dayOfWeek
            habitsList.filter { habit ->
                habit.scheduledDays.contains(today)
            }
        } else {
            habitsList
        }

        // Sort habits according to user order
        val sortedHabits = filteredList.sortedBy { habit ->
            _habitOrder.value.indexOf(habit.id).let {
                if (it >= 0) it else Int.MAX_VALUE
            }
        }

        _habits.value = sortedHabits
    }

    // Check if a habit is upcoming (for UI display)
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
            android.util.Log.e("HabitViewModel", "Error checking if habit is upcoming: ${e.message}")
            return false
        }
    }

    // Helper method to determine if a day is after today
    private fun isDateAfterToday(today: LocalDate, dayOfWeek: DayOfWeek): Boolean {
        val daysUntilNext = (dayOfWeek.value - today.dayOfWeek.value + 7) % 7
        return daysUntilNext > 0
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
            val habit = _allHabits.value.find { it.id == id }
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
            val habit = _allHabits.value.find { it.id == id } ?: return@launch
            alarmUtils.cancelAlarm(habit.id)
            scheduleHabitAlarm(habit.copy(vibrationEnabled = vibrationEnabled))
        }
    }

    fun updateSnoozeSetting(id: String, snoozeEnabled: Boolean) {
        viewModelScope.launch {
            habitRepository.updateSnoozeSetting(id, snoozeEnabled)

            // Update alarm
            val habit = _allHabits.value.find { it.id == id } ?: return@launch
            alarmUtils.cancelAlarm(habit.id)
            scheduleHabitAlarm(habit.copy(snoozeEnabled = snoozeEnabled))
        }
    }

    fun completeHabit(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            habitRepository.completeHabit(id, isCompleted)

            // If marked as completed, cancel the alarm and record history
            if (isCompleted) {
                alarmUtils.cancelAlarm(id)

                // Record completion in history
                val habit = _allHabits.value.find { it.id == id }
                if (habit != null) {
                    val today = LocalDate.now()
                    val timestamp = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val historyEntry = HabitHistory(
                        habitId = habit.id,
                        date = timestamp,
                        status = HabitStatus.COMPLETED
                    )

                    habitRepository.saveHabitHistory(historyEntry)
                }
            } else {
                // Re-schedule alarm if marked as not completed
                val habit = _allHabits.value.find { it.id == id }
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

    fun deleteCompletedHabits(habits: List<Habit>) {
        viewModelScope.launch {
            // Delete each completed habit
            habits.forEach { habit ->
                habitRepository.deleteHabit(habit)

                // Cancel any alarms
                alarmUtils.cancelAlarm(habit.id)
            }
        }
    }

    // Load history for a specific date
    fun loadHistoryForDate(date: LocalDate) {
        viewModelScope.launch {
            try {
                val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

                habitRepository.getHabitHistoryForDateRange(startOfDay, endOfDay)
                    .collect { historyForDate ->
                        _habitHistory.value = historyForDate
                    }
            } catch (e: Exception) {
                android.util.Log.e("HabitViewModel", "Error loading history for date: ${e.message}")
            }
        }
    }

    // Load history for a date range
    fun loadHistoryForDateRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            try {
                val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

                habitRepository.getHabitHistoryForDateRange(startMillis, endMillis)
                    .collect { historyForRange ->
                        _habitHistory.value = historyForRange
                    }
            } catch (e: Exception) {
                android.util.Log.e("HabitViewModel", "Error loading history for date range: ${e.message}")
            }
        }
    }
}