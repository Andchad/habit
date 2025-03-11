package com.andchad.habit.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add vibrationEnabled and snoozeEnabled columns with default values
        database.execSQL("ALTER TABLE habits ADD COLUMN vibrationEnabled INTEGER NOT NULL DEFAULT 1") // 1 = true
        database.execSQL("ALTER TABLE habits ADD COLUMN snoozeEnabled INTEGER NOT NULL DEFAULT 1")    // 1 = true
    }
}