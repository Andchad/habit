package com.andchad.habit.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andchad.habit.data.model.Habit

@Database(entities = [Habit::class], version = 3, exportSchema = false)
@TypeConverters(DayOfWeekTypeConverter::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}