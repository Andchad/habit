package com.andchad.habit.utils

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andchad.habit.data.HabitRepository
import com.andchad.habit.data.model.HabitHistory
import com.andchad.habit.data.model.HabitStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class HabitHistoryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // Get all habits
            val habits = habitRepository.getHabits().first()

            // Get today's date (just the date part, not time)
            val today = LocalDate.now()
            val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Get the day of week for today
            val dayOfWeek = today.dayOfWeek

            // Filter habits scheduled for today
            val todaysHabits = habits.filter { habit ->
                habit.scheduledDays.contains(dayOfWeek)
            }

            Log.d("HabitHistoryWorker", "Recording status for ${todaysHabits.size} habits")

            // Record status for each habit
            todaysHabits.forEach { habit ->
                val status = if (habit.isCompleted) HabitStatus.COMPLETED else HabitStatus.MISSED

                val habitHistory = HabitHistory(
                    habitId = habit.id,
                    date = todayMillis,
                    status = status
                )

                habitRepository.saveHabitHistory(habitHistory)

                // Reset completion status for the next day
                if (habit.isCompleted) {
                    habitRepository.completeHabit(habit.id, false)
                }

                Log.d("HabitHistoryWorker", "Recorded habit ${habit.name} as ${status}")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("HabitHistoryWorker", "Error recording habit history: ${e.message}")
            return Result.failure()
        }
    }
}