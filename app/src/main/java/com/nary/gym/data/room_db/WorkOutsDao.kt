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

    @Query("SELECT * FROM workouts WHERE syncState != 'PENDING_DELETE' AND (ownerId = :owner OR ownerId IS NULL) ORDER BY id")
    fun getAllWorkouts(owner: String): Flow<List<WorkOuts>>

    @Query("SELECT * FROM workouts WHERE workOutType = :type AND syncState != 'PENDING_DELETE' AND (ownerId = :owner OR ownerId IS NULL)")
    fun getWorkoutsByType(type: WorkOutType, owner: String): Flow<List<WorkOuts>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: String): WorkOuts?

    @Query("SELECT * FROM workouts WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): WorkOuts?

    @Query("SELECT * FROM workouts WHERE syncState = 'SYNCED' AND ownerId = :owner")
    suspend fun syncedRows(owner: String): List<WorkOuts>

    @Query("SELECT * FROM workouts WHERE syncState != 'SYNCED' AND syncError IS NULL AND ownerId = :owner ORDER BY id")
    suspend fun pendingForSync(owner: String): List<WorkOuts>

    @Query("UPDATE workouts SET ownerId = :owner WHERE ownerId IS NULL")
    suspend fun claimUnowned(owner: String)

    @Query(
        "UPDATE workouts SET serverId = :serverId, " +
            "syncState = CASE WHEN version = :version THEN 'SYNCED' ELSE 'PENDING_UPDATE' END " +
            "WHERE id = :id"
    )
    suspend fun finishCreate(id: String, serverId: String, version: Int): Int

    @Query("UPDATE workouts SET syncState = 'SYNCED' WHERE id = :id AND version = :version")
    suspend fun finishUpdate(id: String, version: Int)

    @Query("UPDATE workouts SET serverId = :serverId, syncState = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun adoptServerId(id: String, serverId: String)

    @Query("UPDATE workouts SET serverId = NULL, syncState = 'PENDING_CREATE' WHERE id = :id")
    suspend fun markForRecreate(id: String)

    @Query("UPDATE workouts SET syncError = :message WHERE id = :id")
    suspend fun markRejected(id: String, message: String)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)

    suspend fun insertTombstone(serverId: String, owner: String) = insert(
        WorkOuts(
            id = serverId,
            workOutName = "",
            workOutType = WorkOutType.LEG,
            workOutImage = "",
            serverId = serverId,
            syncState = SyncState.PENDING_DELETE,
            ownerId = owner,
        )
    )

    @Transaction
    suspend fun saveLocal(workout: WorkOuts) {
        val existing = getById(workout.id)
        if (existing == null) {
            insert(workout.copy(serverId = null, syncState = SyncState.PENDING_CREATE, version = 1, syncError = null))
        } else {
            insert(
                existing.copy(
                    workOutName = workout.workOutName,
                    workOutType = workout.workOutType,
                    workOutImage = workout.workOutImage,
                    syncState = if (existing.syncState == SyncState.SYNCED) SyncState.PENDING_UPDATE else existing.syncState,
                    version = existing.version + 1,
                    syncError = null,
                    ownerId = existing.ownerId ?: workout.ownerId,
                )
            )
        }
    }

    @Transaction
    suspend fun deleteLocal(id: String) {
        val existing = getById(id) ?: return
        if (existing.serverId == null) {
            deleteById(id)
        } else {
            insert(existing.copy(syncState = SyncState.PENDING_DELETE, version = existing.version + 1, syncError = null))
        }
    }

    @Transaction
    suspend fun mergeFromServer(server: List<WorkOuts>, owner: String) {
        val serverIds = server.mapNotNull { it.serverId }.toSet()
        syncedRows(owner).filter { it.serverId !in serverIds }.forEach { deleteById(it.id) }
        server.forEach { remote ->
            val serverId = remote.serverId ?: return@forEach
            val local = getByServerId(serverId)
            when {
                local == null -> {
                    val sameId = getById(serverId)
                    if (sameId == null || sameId.syncState == SyncState.SYNCED) {
                        insert(remote.copy(id = serverId, syncState = SyncState.SYNCED, version = 0, syncError = null, ownerId = owner))
                    }
                }
                local.syncState == SyncState.SYNCED -> insert(
                    local.copy(
                        workOutName = remote.workOutName,
                        workOutType = remote.workOutType,
                        workOutImage = remote.workOutImage,
                        ownerId = owner,
                    )
                )
                else -> Unit
            }
        }
    }
}
