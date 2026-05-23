package com.example.narytodo.di

import android.content.Context

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.data.room_db.WorkOutsDao
import com.example.narytodo.data.room_db.WorkOutsDay
import com.example.narytodo.data.room_db.WorkoutDatabase

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

                            prePopulate(database)
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

    private suspend fun prePopulate(
        database: WorkoutDatabase
    ) {
        val dao = database.workoutDao()
        val days = listOf(
            WorkOutsDay(
                id = 1,
                workOutType = WorkOutType.SHOULDER
            ),
            WorkOutsDay(
                id = 2,
                workOutType = WorkOutType.LEG
            ),
            WorkOutsDay(
                id = 3,
                workOutType = WorkOutType.CHEST
            ),
            WorkOutsDay(
                id = 4,
                workOutType = WorkOutType.BACK
            )
        )

        dao.insertAllDays(days)
        val workouts = listOf(
            // Day 1 - SHOULDER
            WorkOuts(workOutName = "Machine Shoulder Press", workOutType = WorkOutType.SHOULDER, workOutImage = "machine_shoulder_press", dayId = 1),
            WorkOuts(workOutName = "Lateral Raise",          workOutType = WorkOutType.SHOULDER, workOutImage = "lateral_raise",          dayId = 1),
            WorkOuts(workOutName = "Front Raise",            workOutType = WorkOutType.SHOULDER, workOutImage = "front_raise",            dayId = 1),
            WorkOuts(workOutName = "Machine Rear Delt Fly",  workOutType = WorkOutType.SHOULDER, workOutImage = "machine_rear_delt_fly",  dayId = 1),
            // Day 2 - Legs
            WorkOuts(workOutName = "Leg Extension",  workOutType = WorkOutType.LEG, workOutImage = "leg_extension",  dayId = 2),
            WorkOuts(workOutName = "Leg Press",      workOutType = WorkOutType.LEG, workOutImage = "leg_press",      dayId = 2),
            WorkOuts(workOutName = "Lying Curls",    workOutType = WorkOutType.LEG, workOutImage = "lying_curls",    dayId = 2),
            WorkOuts(workOutName = "Squats",         workOutType = WorkOutType.LEG, workOutImage = "squats",         dayId = 2),
            // Day 3 - Chest & Triceps
            WorkOuts(workOutName = "Machine Incline Chest Press", workOutType = WorkOutType.CHEST, workOutImage = "machine_incline_chest_press", dayId = 3),
            WorkOuts(workOutName = "Machine Pec Fly",             workOutType = WorkOutType.CHEST, workOutImage = "machine_pec_fly",             dayId = 3),
            WorkOuts(workOutName = "Machine Flat Press",          workOutType = WorkOutType.CHEST, workOutImage = "machine_flat_press",          dayId = 3),
            WorkOuts(workOutName = "Push Down",                   workOutType = WorkOutType.CHEST, workOutImage = "push_down",                   dayId = 3),
            WorkOuts(workOutName = "Seated Dips",                 workOutType = WorkOutType.CHEST, workOutImage = "seated_dips",                 dayId = 3),
            // Day 4 - Back & Biceps
            WorkOuts(workOutName = "Lat Pulldown",   workOutType = WorkOutType.BACK, workOutImage = "lat_pulldown",   dayId = 4),
            WorkOuts(workOutName = "Seated Rowing",  workOutType = WorkOutType.BACK, workOutImage = "seated_rowing",  dayId = 4),
            WorkOuts(workOutName = "One Arm Rowing", workOutType = WorkOutType.BACK, workOutImage = "one_arm_rowing", dayId = 4),
            WorkOuts(workOutName = "Dumbbell Curls", workOutType = WorkOutType.BACK, workOutImage = "dumbbell_curls", dayId = 4),
            WorkOuts(workOutName = "Preacher Curls", workOutType = WorkOutType.BACK, workOutImage = "preacher_curls", dayId = 4),
        )
        dao.insertAllWorkouts(workouts)
    }
}