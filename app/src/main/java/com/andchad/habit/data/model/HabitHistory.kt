package com.andchad.habit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.andchad.habit.data.HabitStatusConverter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Represents a historical record of a habit's completion status for a specific date.
 *
 * @param id Unique identifier for this history record
 * @param habitId The ID of the habit this history belongs to
 * @param date The timestamp (in milliseconds) for the day this record is for
 * @param status Whether the habit was completed or missed for this date
 */
@Entity(tableName = "habit_history")
data class HabitHistory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: Long, // Timestamp for the day
    val status: HabitStatus
) {
    /**
     * Converts the timestamp to a formatted date string
     */
    fun getFormattedDate(): String {
        val localDate = Instant.ofEpochMilli(date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return localDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    }

    /**
     * Checks if this history record is from today
     */
    fun isToday(): Boolean {
        val historyDate = Instant.ofEpochMilli(date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return historyDate == LocalDate.now()
    }

    /**
     * Returns the day of the week for this history record
     */
    fun getDayOfWeek(): String {
        val historyDate = Instant.ofEpochMilli(date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return historyDate.dayOfWeek.toString()
    }
}

/**
 * Represents the possible status values for a habit on a specific day.
 */
enum class HabitStatus {
    COMPLETED,
    MISSED
}