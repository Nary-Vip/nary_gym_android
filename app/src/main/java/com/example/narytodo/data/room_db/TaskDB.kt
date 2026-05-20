package com.example.narytodo.data.room_db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.narytodo.core.WorkOutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkOutsDao

    companion object {

        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                ).addCallback(object: RoomDatabase.Callback(){
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let {
                                prePopulate(it)
                            }
                        }

                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prePopulate(database: WorkoutDatabase) {
            val dao = database.workoutDao()

            // Insert Days
            val days = listOf(
                WorkOutsDay(id = 1, workOutType = WorkOutType.SHOULDER),
                WorkOutsDay(id = 2, workOutType = WorkOutType.LEG),
                WorkOutsDay(id = 3, workOutType = WorkOutType.CHEST),
                WorkOutsDay(id = 4, workOutType = WorkOutType.BACK)
            )
            dao.insertAllDays(days)

            // Insert Workouts
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
}