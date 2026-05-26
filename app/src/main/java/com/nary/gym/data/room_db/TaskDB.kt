package com.nary.gym.data.room_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskItem:: class], version = 1)
abstract class TaskDataBase: RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDataBase? = null

        fun getDatabase(context: Context): TaskDataBase{
            return INSTANCE ?: synchronized(this){
                Room.databaseBuilder(
                    context.applicationContext,
                    TaskDataBase:: class.java,
                    "tasks_databases"
                    ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it}
            }
        }
    }
}

@Database(
    entities = [
        WorkOuts::class,
        WorkOutsDay::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkOutsDao
}