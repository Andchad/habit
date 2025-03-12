package com.andchad.habit.data

import com.andchad.habit.data.model.Habit
import com.andchad.habit.data.model.HabitHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.time.DayOfWeek
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val database: HabitDatabase  // Inject the database directly
) {
    // Use the database to get the DAOs
    private val habitHistoryDao: HabitHistoryDao
        get() = database.habitHistoryDao()

    // Original methods remain the same

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

    // Get all habit history
    fun getHabitHistory(): Flow<List<HabitHistory>> {
        return try {
            habitHistoryDao.getAllHabitHistory()
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error getting habit history: ${e.message}")
            flowOf(emptyList()) // Return empty flow on error
        }
    }

    // Get history for a specific habit
    fun getHabitHistoryForHabit(habitId: String): Flow<List<HabitHistory>> {
        return try {
            habitHistoryDao.getHabitHistoryForHabit(habitId)
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error getting habit history for habit: ${e.message}")
            flowOf(emptyList())
        }
    }

    // Get history for a specific date
    fun getHabitHistoryForDate(date: Long): Flow<List<HabitHistory>> {
        return try {
            habitHistoryDao.getHabitHistoryForDate(date)
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error getting habit history for date: ${e.message}")
            flowOf(emptyList())
        }
    }

    // Get history for a date range
    fun getHabitHistoryForDateRange(startDate: Long, endDate: Long): Flow<List<HabitHistory>> {
        return try {
            habitHistoryDao.getHabitHistoryBetweenDates(startDate, endDate)
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error getting habit history for date range: ${e.message}")
            flowOf(emptyList())
        }
    }

    // Save a single habit history entry
    suspend fun saveHabitHistory(habitHistory: HabitHistory) {
        try {
            habitHistoryDao.insertHabitHistory(habitHistory)
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error saving habit history: ${e.message}")
        }
    }

    // Record a batch of habit histories
    suspend fun recordHabitHistories(habitHistories: List<HabitHistory>) {
        try {
            habitHistories.forEach { habitHistory ->
                habitHistoryDao.insertHabitHistory(habitHistory)
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Error recording habit histories: ${e.message}")
        }
    }
}