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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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

    init {
        // Observe habits from repository
        habitRepository.getHabits()
            .onEach { habitsList ->
                _habits.value = habitsList
            }
            .launchIn(viewModelScope)
    }

    // Check if a habit with this name already exists
    suspend fun habitNameExists(name: String): Boolean {
        return habitRepository.habitWithNameExists(name)
    }

    fun createHabit(name: String, reminderTime: String) {
        viewModelScope.launch {
            val habit = habitRepository.createHabit(name, reminderTime)
            // Schedule notification
            scheduleHabitReminder(habit)
        }
    }

    fun updateHabit(id: String, name: String, reminderTime: String) {
        viewModelScope.launch {
            habitRepository.updateHabit(id, name, reminderTime)
            // Update notification
            val habit = _habits.value.find { it.id == id }
            if (habit != null) {
                // Cancel the existing reminder
                NotificationUtils.cancelHabitReminder(getApplication(), habit.id)

                // Schedule with the new time
                scheduleHabitReminder(habit.copy(
                    name = name,
                    reminderTime = reminderTime
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
                habit.reminderTime
            )
        }
    }
}