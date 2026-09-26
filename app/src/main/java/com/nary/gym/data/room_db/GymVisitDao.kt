package com.nary.gym.data.room_db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymVisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: GymVisit)

    @Update
    suspend fun update(visit: GymVisit)

    @Query("DELETE FROM gym_visits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM gym_visits WHERE checkOutAt IS NULL ORDER BY checkInAt DESC LIMIT 1")
    suspend fun getOpenVisit(): GymVisit?

    @Query("SELECT * FROM gym_visits ORDER BY checkInAt DESC")
    fun observeAll(): Flow<List<GymVisit>>

    @Query("SELECT * FROM gym_visits WHERE checkOutAt IS NULL ORDER BY checkInAt DESC LIMIT 1")
    fun observeOpenVisit(): Flow<GymVisit?>
}
