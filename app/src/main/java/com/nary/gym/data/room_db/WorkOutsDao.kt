package com.nary.gym.data.room_db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nary.gym.core.WorkOutType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOutsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkOuts)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWorkouts(workouts: List<WorkOuts>)

    @Update
    suspend fun update(workOuts: WorkOuts)

    @Delete
    suspend fun delete(workOuts: WorkOuts)

    @Query(value = "SELECT * FROM workouts ORDER BY id")
    fun getAllWorkouts(): Flow<List<WorkOuts>>

    @Query("SELECT * FROM workouts WHERE workOutType = :type")
    fun getWorkoutsByType(type: WorkOutType): Flow<List<WorkOuts>>

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()

    @Query("DELETE FROM workouts WHERE id NOT IN (:ids)")
    suspend fun deleteWorkoutsNotIn(ids: List<String>)

    /** Replaces the full local workout set with [workouts], removing anything no longer on the server. */
    @Transaction
    suspend fun replaceAllWorkouts(workouts: List<WorkOuts>) {
        insertAllWorkouts(workouts)
        if (workouts.isEmpty()) {
            deleteAll()
        } else {
            deleteWorkoutsNotIn(workouts.map { it.id })
        }
    }
}
