package com.andchad.habit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andchad.habit.data.HabitRepository
import com.andchad.habit.data.model.Habit
import com.andchad.habit.utils.NotificationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // User-defined order for habits
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

    // Move a habit from one position to another
    fun moveHabit(fromIndex: Int, toIndex: Int) {
        val currentOrder = _habitOrder.value.toMutableList()
        if (fromIndex < currentOrder.size && toIndex < currentOrder.size) {
            val habitId = currentOrder.removeAt(fromIndex)
            currentOrder.add(toIndex, habitId)
            _habitOrder.value = currentOrder

            // Update the sorted list
            val currentHabits = _habits.value
            _habits.value = currentHabits.sortedBy { habit ->
                currentOrder.indexOf(habit.id).let {
                    if (it >= 0) it else Int.MAX_VALUE
                }
            }
        }
    }

    // Check if a habit with this name already exists
    suspend fun habitNameExists(name: String): Boolean {
        return habitRepository.habitWithNameExists(name)
    }

    fun createHabit(name: String, reminderTime: String, scheduledDays: List<DayOfWeek>) {
        viewModelScope.launch {
            val habit = habitRepository.createHabit(name, reminderTime, scheduledDays)
            // Schedule notification
            scheduleHabitReminder(habit)
        }
    }

    fun updateHabit(id: String, name: String, reminderTime: String, scheduledDays: List<DayOfWeek>) {
        viewModelScope.launch {
            habitRepository.updateHabit(id, name, reminderTime, scheduledDays)
            // Update notification
            val habit = _habits.value.find { it.id == id }
            if (habit != null) {
                // Cancel the existing reminder
                NotificationUtils.cancelHabitReminder(getApplication(), habit.id)

                // Schedule with the new details
                scheduleHabitReminder(habit.copy(
                    name = name,
                    reminderTime = reminderTime,
                    scheduledDays = scheduledDays
                ))
            }
        }
    }

    fun completeHabit(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            habitRepository.completeHabit(id, isCompleted)

            // If marked as completed, cancel the reminder
            if (isCompleted) {
                NotificationUtils.cancelHabitReminder(getApplication(), id)
            } else {
                // Re-schedule reminder if marked as not completed
                val habit = _habits.value.find { it.id == id }
                if (habit != null) {
                    scheduleHabitReminder(habit.copy(isCompleted = false))
                }
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)

            // Cancel notification
            NotificationUtils.cancelHabitReminder(getApplication(), habit.id)
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    private fun scheduleHabitReminder(habit: Habit) {
        // Only schedule if habit is not completed
        if (!habit.isCompleted) {
            NotificationUtils.scheduleHabitReminder(
                getApplication(),
                habit.id,
                habit.name,
                habit.reminderTime,
                habit.scheduledDays
            )
        }
    }
}