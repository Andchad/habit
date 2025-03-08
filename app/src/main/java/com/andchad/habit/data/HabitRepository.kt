package com.andchad.habit.data

import com.andchad.habit.data.model.Habit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val firestore: FirebaseFirestore = Firebase.firestore
) {
    private val habitsCollection = firestore.collection("habits")

    // Get all habits (combined from local and Firestore)
    fun getHabits(): Flow<List<Habit>> {
        val localHabits = habitDao.getHabits()
        val remoteHabits = getRemoteHabits()

        return localHabits.combine(remoteHabits) { local, remote ->
            // In a real app, you might want to implement a more sophisticated sync mechanism
            // For simplicity, we'll just return local data, as it will be updated by the remote data
            local
        }
    }

    // Check if a habit with this name already exists
    suspend fun habitWithNameExists(name: String): Boolean {
        // For checking in the database
        val habitsFromFlow = habitDao.getHabits().first()

        // Check if any habit name matches (case-insensitive)
        return habitsFromFlow.any { it.name.equals(name, ignoreCase = true) }
    }

    // Create a new habit
    suspend fun createHabit(name: String, reminderTime: String): Habit {
        val habitId = UUID.randomUUID().toString()
        val habit = Habit(
            id = habitId,
            name = name,
            reminderTime = reminderTime
        )

        // Add to local database
        habitDao.insertHabit(habit)

        // Add to Firestore
        habitsCollection.document(habitId)
            .set(Habit.toFirestore(habit))
            .await()

        return habit
    }

    // Update habit details
    suspend fun updateHabit(id: String, name: String, reminderTime: String) {
        // Update in local database
        habitDao.updateHabitDetails(id, name, reminderTime)

        // Update in Firestore
        habitsCollection.document(id)
            .update(
                mapOf(
                    "name" to name,
                    "reminderTime" to reminderTime
                )
            )
            .await()
    }

    // Mark habit as completed
    suspend fun completeHabit(id: String, isCompleted: Boolean) {
        // Update in local database
        habitDao.updateHabitCompletion(id, isCompleted)

        // Update in Firestore
        habitsCollection.document(id)
            .update("isCompleted", isCompleted)
            .await()
    }

    // Delete a habit
    suspend fun deleteHabit(habit: Habit) {
        // Delete from local database
        habitDao.deleteHabit(habit)

        // Delete from Firestore
        habitsCollection.document(habit.id)
            .delete()
            .await()
    }

    // Get habits from Firestore
    private fun getRemoteHabits(): Flow<List<Habit>> = callbackFlow {
        val listenerRegistration = habitsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Handle error
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val habits = snapshot.documents.mapNotNull { document ->
                    val data = document.data
                    if (data != null) {
                        Habit.fromFirestore(data, document.id)
                    } else {
                        null
                    }
                }

                // Send the habits to the flow
                trySend(habits)

                // Update local database with Firestore data
                // Launch a coroutine to handle the suspending functions
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    for (habit in habits) {
                        // This would be better done in a transaction, but for simplicity we'll just insert each one
                        habitDao.insertHabit(habit)
                    }
                }
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }
}