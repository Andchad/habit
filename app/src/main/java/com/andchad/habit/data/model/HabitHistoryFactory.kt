package com.andchad.habit.data.model

import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Factory class for creating HabitHistory objects.
 * Makes it easier to create history entries with consistent timestamps.
 */
object HabitHistoryFactory {
    /**
     * Creates a HabitHistory entry for the specified date
     *
     * @param habitId The ID of the habit
     * @param status Whether the habit was completed or missed
     * @param date The date for this history (defaults to today)
     * @return A new HabitHistory object
     */
    fun create(
        habitId: String,
        status: HabitStatus,
        date: LocalDate = LocalDate.now()
    ): HabitHistory {
        // Convert LocalDate to milliseconds timestamp at start of day
        val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return HabitHistory(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            date = timestamp,
            status = status
        )
    }

    /**
     * Creates a HabitHistory entry for today
     *
     * @param habitId The ID of the habit
     * @param isCompleted Whether the habit was completed (true) or missed (false)
     * @return A new HabitHistory object
     */
    fun createForToday(habitId: String, isCompleted: Boolean): HabitHistory {
        val status = if (isCompleted) HabitStatus.COMPLETED else HabitStatus.MISSED
        return create(habitId, status)
    }

    /**
     * Creates a batch of history entries for a habit over multiple days
     *
     * @param habitId The ID of the habit
     * @param startDate The starting date
     * @param days Number of days to create entries for
     * @param status The status to set for all entries
     * @return List of HabitHistory objects
     */
    fun createBatch(
        habitId: String,
        startDate: LocalDate,
        days: Int,
        status: HabitStatus
    ): List<HabitHistory> {
        return (0 until days).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            create(habitId, status, date)
        }
    }
}