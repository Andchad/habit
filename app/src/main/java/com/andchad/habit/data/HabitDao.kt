package com.andchad.habit.data

import androidx.room.*
import com.andchad.habit.data.model.Habit
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("UPDATE habits SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateHabitCompletion(id: String, isCompleted: Boolean)

    @Query("UPDATE habits SET name = :name, reminderTime = :reminderTime WHERE id = :id")
    suspend fun updateHabitDetails(id: String, name: String, reminderTime: String)

    @Query("UPDATE habits SET name = :name, reminderTime = :reminderTime, scheduledDays = :scheduledDays WHERE id = :id")
    suspend fun updateHabitDetailsWithSchedule(id: String, name: String, reminderTime: String, scheduledDays: List<DayOfWeek>)

    @Query("UPDATE habits SET name = :name, reminderTime = :reminderTime, scheduledDays = :scheduledDays, vibrationEnabled = :vibrationEnabled, snoozeEnabled = :snoozeEnabled WHERE id = :id")
    suspend fun updateHabitDetailsWithAlarmSettings(
        id: String,
        name: String,
        reminderTime: String,
        scheduledDays: List<DayOfWeek>,
        vibrationEnabled: Boolean,
        snoozeEnabled: Boolean
    )

    @Query("UPDATE habits SET vibrationEnabled = :vibrationEnabled WHERE id = :id")
    suspend fun updateVibrationSetting(id: String, vibrationEnabled: Boolean)

    @Query("UPDATE habits SET snoozeEnabled = :snoozeEnabled WHERE id = :id")
    suspend fun updateSnoozeSetting(id: String, snoozeEnabled: Boolean)
}