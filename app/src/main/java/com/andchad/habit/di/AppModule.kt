package com.andchad.habit.di

import android.content.Context
import androidx.room.Room
import com.andchad.habit.data.HabitDao
import com.andchad.habit.data.HabitDatabase
import com.andchad.habit.data.HabitRepository
import com.andchad.habit.data.MIGRATION_1_2
import com.andchad.habit.data.MIGRATION_2_3
import com.andchad.habit.utils.AdManager
import com.andchad.habit.utils.AlarmUtils
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
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    @Singleton
    fun provideHabitRepository(
        habitDao: HabitDao
    ): HabitRepository {
        return HabitRepository(habitDao)
    }

    @Provides
    @Singleton
    fun provideAdManager(
        @ApplicationContext context: Context
    ): AdManager {
        return AdManager(context)
    }

    @Provides
    @Singleton
    fun provideAlarmUtils(
        @ApplicationContext context: Context
    ): AlarmUtils {
        return AlarmUtils(context)
    }
}