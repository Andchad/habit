package com.andchad.habit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey
    val id: String,
    val name: String,
    val reminderTime: String, // Stored as "HH:mm" format
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val scheduledDays: List<DayOfWeek> = emptyList(), // Days of week when the habit is scheduled
    val vibrationEnabled: Boolean = true,  // Default to enabled
    val snoozeEnabled: Boolean = true      // Default to enabled
) {
    fun getReminderTimeAsLocalTime(): LocalTime {
        return LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
    }

    // Get formatted days of week for display
    fun getFormattedScheduledDays(): String {
        if (scheduledDays.isEmpty()) return "No days selected"

        // Short day names
        val sortedDays = scheduledDays.sortedBy { it.value }
        val dayLabels = sortedDays.map { it.name.substring(0, 3) }

        // Group consecutive days
        if (dayLabels.size >= 3) {
            val consecutive = mutableListOf<String>()
            var start = sortedDays.first()
            var end = start

            for (i in 1 until sortedDays.size) {
                val current = sortedDays[i]
                if (current.value == end.value + 1) {
                    end = current
                } else {
                    if (start == end) {
                        consecutive.add(start.name.substring(0, 3))
                    } else if (end.value - start.value == 1) {
                        consecutive.add("${start.name.substring(0, 3)}, ${end.name.substring(0, 3)}")
                    } else {
                        consecutive.add("${start.name.substring(0, 3)}-${end.name.substring(0, 3)}")
                    }
                    start = current
                    end = current
                }
            }

            if (start == end) {
                consecutive.add(start.name.substring(0, 3))
            } else if (end.value - start.value == 1) {
                consecutive.add("${start.name.substring(0, 3)}, ${end.name.substring(0, 3)}")
            } else {
                consecutive.add("${start.name.substring(0, 3)}-${end.name.substring(0, 3)}")
            }

            return consecutive.joinToString(", ")
        }

        return dayLabels.joinToString(", ")
    }
}