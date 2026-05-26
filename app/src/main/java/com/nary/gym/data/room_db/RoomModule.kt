package com.nary.gym.di

import android.content.Context

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

import com.nary.gym.data.room_db.WorkOutsDao
import com.nary.gym.data.room_db.WorkoutDatabase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            .fallbackToDestructiveMigration(false)
            .addCallback(
                object : RoomDatabase.Callback() {

                    override fun onCreate(
                        db: SupportSQLiteDatabase
                    ) {
                        super.onCreate(db)

                        CoroutineScope(
                            Dispatchers.IO
                        ).launch {

                            val database =
                                Room.databaseBuilder(
                                    context,
                                    WorkoutDatabase::class.java,
                                    "workout_database"
                                ).build()
                        }
                    }
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(
        database: WorkoutDatabase
    ): WorkOutsDao {
        return database.workoutDao()
    }
}