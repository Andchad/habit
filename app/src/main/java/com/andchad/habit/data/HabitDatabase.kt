package com.andchad.habit.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andchad.habit.data.model.Habit
import androidx.room.*
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

@Database(entities = [Habit::class], version = 2, exportSchema = false)
@TypeConverters(DayOfWeekTypeConverter::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}

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
}