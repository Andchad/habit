package com.andchad.habit.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andchad.habit.data.model.Habit
import com.andchad.habit.data.model.HabitHistory

@Database(entities = [Habit::class, HabitHistory::class], version = 4, exportSchema = false)
@TypeConverters(DayOfWeekTypeConverter::class, HabitStatusConverter::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitHistoryDao(): HabitHistoryDao
}

// Define migration from version 3 to 4
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `habit_history` (" +
                    "`id` TEXT NOT NULL, " +
                    "`habitId` TEXT NOT NULL, " +
                    "`date` INTEGER NOT NULL, " +
                    "`status` TEXT NOT NULL, " +  // Changed from INTEGER to TEXT
                    "PRIMARY KEY(`id`))"
        )
    }
}