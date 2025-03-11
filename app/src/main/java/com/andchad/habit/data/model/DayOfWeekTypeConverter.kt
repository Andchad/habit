package com.andchad.habit.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class DayOfWeekTypeConverter {
    @TypeConverter
    fun fromDayOfWeekList(value: List<DayOfWeek>): String {
        return value.joinToString(",") { it.value.toString() }
    }

    @TypeConverter
    fun toDayOfWeekList(value: String): List<DayOfWeek> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { DayOfWeek.of(it.toInt()) }
    }
}