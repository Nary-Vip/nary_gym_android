package com.nary.gym.di

import android.content.Context

import androidx.room.Room

import com.nary.gym.data.room_db.GymVisitDao
import com.nary.gym.data.room_db.MIGRATION_5_6
import com.nary.gym.data.room_db.MIGRATION_6_7
import com.nary.gym.data.room_db.MIGRATION_7_8
import com.nary.gym.data.room_db.WorkOutsDao
import com.nary.gym.data.room_db.WorkoutDatabase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideWorkoutDatabase(
        @ApplicationContext
        context: Context
    ): WorkoutDatabase {

        return Room.databaseBuilder(
            context,
            WorkoutDatabase::class.java,
            "workout_database"
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(
        database: WorkoutDatabase
    ): WorkOutsDao {
        return database.workoutDao()
    }

    @Provides
    @Singleton
    fun provideGymVisitDao(
        database: WorkoutDatabase
    ): GymVisitDao {
        return database.gymVisitDao()
    }
}
