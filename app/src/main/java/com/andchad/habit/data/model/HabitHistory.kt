package com.andchad.habit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.andchad.habit.data.HabitStatusConverter
import java.util.UUID

@Entity(tableName = "habit_history")
data class HabitHistory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: Long, // Timestamp for the day
    val status: HabitStatus
)

enum class HabitStatus {
    COMPLETED,
    MISSED
}