package com.andchad.habit.data

import androidx.room.TypeConverter
import com.andchad.habit.data.model.HabitStatus

/**
 * Type converter for Room database to convert between HabitStatus enum and String.
 * This allows storing enum values in the SQLite database, which doesn't natively support enums.
 */
class HabitStatusConverter {
    /**
     * Converts HabitStatus enum to String for database storage
     */
    @TypeConverter
    fun fromHabitStatus(status: HabitStatus): String {
        return status.name
    }

    /**
     * Converts String from database back to HabitStatus enum
     */
    @TypeConverter
    fun toHabitStatus(statusString: String): HabitStatus {
        return try {
            HabitStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            // Fallback in case of invalid values
            HabitStatus.MISSED
        }
    }
}