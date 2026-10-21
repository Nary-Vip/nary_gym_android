package com.nary.gym.data.room_db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        WorkOuts::class,
        GymVisit::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkOutsDao

    abstract fun gymVisitDao(): GymVisitDao
}
