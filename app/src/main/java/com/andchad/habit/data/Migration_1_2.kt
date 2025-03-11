package com.andchad.habit.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the scheduledDays column to the habits table
        database.execSQL("ALTER TABLE habits ADD COLUMN scheduledDays TEXT NOT NULL DEFAULT ''")
    }
}