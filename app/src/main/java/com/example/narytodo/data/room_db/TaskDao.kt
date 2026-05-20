package com.example.narytodo.data.room_db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.narytodo.core.WorkOutType
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItem)

    @Update
    suspend fun update(task: TaskItem)

    @Delete
    suspend fun delete(task: TaskItem)

    @Query(value = "SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskItem>>
}

@Dao
interface WorkOutsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkOuts)

    @Update
    suspend fun update(workOuts: WorkOuts)

    @Delete
    suspend fun delete(workOuts: WorkOuts)

    @Query(value = "SELECT * FROM workouts ORDER BY id")
    fun getAllWorkouts(): Flow<List<WorkOuts>>

    @Query("SELECT * FROM workouts WHERE workOutType = :type")
    fun getWorkoutsByType(type: WorkOutType): Flow<List<WorkOuts>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllDays(days: List<WorkOutsDay>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllWorkouts(workouts: List<WorkOuts>)
}