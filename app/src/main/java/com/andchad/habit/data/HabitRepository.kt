package com.andchad.habit.data

import com.andchad.habit.data.model.Habit
import com.andchad.habit.data.model.HabitHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitHistoryDao: HabitHistoryDao
) {
    // Get all habits from Room database
    fun getHabits(): Flow<List<Habit>> {
        return habitDao.getHabits()
    }

    // Check if a habit with this name already exists
    suspend fun habitWithNameExists(name: String): Boolean {
        // For checking in the database
        val habitsFromFlow = habitDao.getHabits().first()

        // Check if any habit name matches (case-insensitive)
        return habitsFromFlow.any { it.name.equals(name, ignoreCase = true) }
    }

    // Create a new habit
    suspend fun createHabit(
        name: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean,
        snoozeEnabled: Boolean
    ): Habit {
        val habitId = UUID.randomUUID().toString()
        val habit = Habit(
            id = habitId,
            name = name,
            reminderTime = reminderTime,
            scheduledDays = scheduledDays,
            vibrationEnabled = vibrationEnabled,
            snoozeEnabled = snoozeEnabled
        )

        // Add to local database
        habitDao.insertHabit(habit)

        return habit
    }

    // Update habit details
    suspend fun updateHabit(
        id: String,
        name: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean,
        snoozeEnabled: Boolean
    ) {
        // Update in local database
        habitDao.updateHabitDetailsWithAlarmSettings(
            id,
            name,
            reminderTime,
            scheduledDays,
            vibrationEnabled,
            snoozeEnabled
        )
    }

    // Update just the vibration setting
    suspend fun updateVibrationSetting(id: String, vibrationEnabled: Boolean) {
        habitDao.updateVibrationSetting(id, vibrationEnabled)
    }

    // Update just the snooze setting
    suspend fun updateSnoozeSetting(id: String, snoozeEnabled: Boolean) {
        habitDao.updateSnoozeSetting(id, snoozeEnabled)
    }

    // Mark habit as completed
    suspend fun completeHabit(id: String, isCompleted: Boolean) {
        // Update in local database
        habitDao.updateHabitCompletion(id, isCompleted)
    }

    // Delete a habit
    suspend fun deleteHabit(habit: Habit) {
        // Delete from local database
        habitDao.deleteHabit(habit)
    }

    // HABIT HISTORY METHODS

    // Save habit history
    suspend fun saveHabitHistory(habitHistory: HabitHistory) {
        habitHistoryDao.insertHabitHistory(habitHistory)
    }

    // Get habit history for a specific habit
    fun getHabitHistoryForHabit(habitId: String): Flow<List<HabitHistory>> {
        return habitHistoryDao.getHabitHistoryForHabit(habitId)
    }

    // Get habit history for a specific date
    fun getHabitHistoryForDate(date: Long): Flow<List<HabitHistory>> {
        return habitHistoryDao.getHabitHistoryForDate(date)
    }

    // Get habit history between two dates
    fun getHabitHistoryBetweenDates(startDate: Long, endDate: Long): Flow<List<HabitHistory>> {
        return habitHistoryDao.getHabitHistoryBetweenDates(startDate, endDate)
    }
}