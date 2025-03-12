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

    @Query("SELECT * FROM habit_history WHERE habitId = :habitId")
    fun getHabitHistoryForHabit(habitId: String): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE date BETWEEN :startDate AND :endDate")
    fun getHabitHistoryBetweenDates(startDate: Long, endDate: Long): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE date = :date")
    fun getHabitHistoryForDate(date: Long): Flow<List<HabitHistory>>
}