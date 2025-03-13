package com.andchad.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andchad.habit.data.model.HabitHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitHistory(habitHistory: HabitHistory)

    @Query("SELECT * FROM habit_history ORDER BY date DESC")
    fun getAllHabitHistory(): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE habitId = :habitId ORDER BY date DESC")
    fun getHabitHistoryForHabit(habitId: String): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getHabitHistoryBetweenDates(startDate: Long, endDate: Long): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE date = :date ORDER BY habitId")
    fun getHabitHistoryForDate(date: Long): Flow<List<HabitHistory>>

    @Query("SELECT COUNT(*) FROM habit_history WHERE habitId = :habitId AND status = 'COMPLETED'")
    fun getCompletedCountForHabit(habitId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_history WHERE habitId = :habitId AND status = 'MISSED'")
    fun getMissedCountForHabit(habitId: String): Flow<Int>

    @Query("SELECT * FROM habit_history WHERE habitId = :habitId AND date >= :startDate ORDER BY date ASC LIMIT :streakThreshold")
    fun getRecentHistoryForHabit(habitId: String, startDate: Long, streakThreshold: Int): Flow<List<HabitHistory>>

    @Query("DELETE FROM habit_history")
    suspend fun deleteAllHabitHistory()

    @Query("DELETE FROM habit_history WHERE habitId = :habitId")
    suspend fun deleteHistoryForHabit(habitId: String)
}