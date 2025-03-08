package com.andchad.habit.di

import android.content.Context
import androidx.room.Room
import com.andchad.habit.data.HabitDao
import com.andchad.habit.data.HabitDatabase
import com.andchad.habit.data.HabitRepository
import com.andchad.habit.utils.AdManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHabitDatabase(
        @ApplicationContext context: Context
    ): HabitDatabase {
        return Room.databaseBuilder(
            context,
            HabitDatabase::class.java,
            "habits_database"
        ).build()
    }

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun provideHabitRepository(
        habitDao: HabitDao,
        firestore: FirebaseFirestore
    ): HabitRepository {
        return HabitRepository(habitDao, firestore)
    }

    @Provides
    @Singleton
    fun provideAdManager(
        @ApplicationContext context: Context
    ): AdManager {
        return AdManager(context)
    }
}