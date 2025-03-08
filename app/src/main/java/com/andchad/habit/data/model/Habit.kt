package com.andchad.habit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey
    val id: String,
    val name: String,
    val reminderTime: String, // Stored as "HH:mm" format
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getReminderTimeAsLocalTime(): LocalTime {
        return LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
    }

    companion object {
        fun fromFirestore(data: Map<String, Any>, id: String): Habit {
            return Habit(
                id = id,
                name = data["name"] as String,
                reminderTime = data["reminderTime"] as String,
                isCompleted = data["isCompleted"] as Boolean,
                createdAt = data["createdAt"] as Long
            )
        }

        fun toFirestore(habit: Habit): Map<String, Any> {
            return mapOf(
                "name" to habit.name,
                "reminderTime" to habit.reminderTime,
                "isCompleted" to habit.isCompleted,
                "createdAt" to habit.createdAt
            )
        }
    }
}