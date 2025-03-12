package com.andchad.habit.data

import androidx.room.TypeConverter
import com.andchad.habit.data.model.HabitStatus

class HabitStatusConverter {
    @TypeConverter
    fun fromHabitStatus(status: HabitStatus): String {
        return status.name
    }

    @TypeConverter
    fun toHabitStatus(statusString: String): HabitStatus {
        return HabitStatus.valueOf(statusString)
    }
}