package com.nary.gym.repository.remote

import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.SyncState
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.data.room_db.WorkOutsDao
import com.nary.gym.sync.SyncRequester
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncOutcome { SUCCESS, RETRY }

private const val DUPLICATE_NAME = "A workout with this name already exists"

@Singleton
class WorkOutRepository @Inject constructor(
    private val dao: WorkOutsDao,
    private val remote: WorkoutRemote,
    private val syncRequester: SyncRequester,
) {
    private val mutex = Mutex()

    fun getAllWorkouts(): Flow<List<WorkOuts>> = dao.getAllWorkouts()

    fun getWorkoutsByType(type: WorkOutType): Flow<List<WorkOuts>> = dao.getWorkoutsByType(type)

    suspend fun save(workout: WorkOuts) {
        val row = if (workout.id.isBlank()) workout.copy(id = "local-${UUID.randomUUID()}") else workout
        dao.saveLocal(row)
        syncRequester.request()
    }

    suspend fun delete(workout: WorkOuts) {
        dao.deleteLocal(workout.id)
        syncRequester.request()
    }

    suspend fun sync(): SyncOutcome = mutex.withLock {
        try {
            val pending = dao.pendingForSync()
            pending.filter { it.syncState == SyncState.PENDING_CREATE }.forEach { pushCreate(it.id) }
            pending.filter { it.syncState == SyncState.PENDING_UPDATE }.forEach { pushUpdate(it.id) }
            pending.filter { it.syncState == SyncState.PENDING_DELETE }.forEach { pushDelete(it.id) }
            dao.mergeFromServer(remote.fetchAll())
            SyncOutcome.SUCCESS
        } catch (e: WorkoutRemoteException.Retryable) {
            SyncOutcome.RETRY
        }
    }

    private suspend fun current(id: String, state: SyncState): WorkOuts? =
        dao.getById(id)?.takeIf { it.syncState == state && it.syncError == null }

    private suspend fun pushCreate(id: String) {
        val row = current(id, SyncState.PENDING_CREATE) ?: return
        val serverId = try {
            remote.create(row.workOutName, row.workOutType, row.workOutImage)
        } catch (e: WorkoutRemoteException.Conflict) {
            adoptExisting(row)
            return
        } catch (e: WorkoutRemoteException.Rejected) {
            dao.markRejected(row.id, e.message)
            return
        }
        if (dao.finishCreate(row.id, serverId, row.version) == 0) deleteOrphan(serverId)
    }

    private suspend fun deleteOrphan(serverId: String) {
        try {
            remote.delete(serverId)
        } catch (e: WorkoutRemoteException.NotFound) {
            return
        } catch (e: WorkoutRemoteException) {
            dao.insertTombstone(serverId)
            if (e is WorkoutRemoteException.Retryable) throw e
        }
    }

    private suspend fun adoptExisting(row: WorkOuts) {
        val serverId = remote.findIdByExactName(row.workOutName)
        if (serverId == null) {
            dao.markRejected(row.id, DUPLICATE_NAME)
            return
        }
        dao.getByServerId(serverId)
            ?.takeIf { it.id != row.id && it.syncState == SyncState.SYNCED }
            ?.let { dao.deleteById(it.id) }
        dao.adoptServerId(row.id, serverId)
        pushUpdate(row.id)
    }

    private suspend fun pushUpdate(id: String) {
        val row = current(id, SyncState.PENDING_UPDATE) ?: return
        val serverId = row.serverId ?: return
        try {
            remote.update(serverId, row.workOutName, row.workOutType, row.workOutImage)
            dao.finishUpdate(row.id, row.version)
        } catch (e: WorkoutRemoteException.NotFound) {
            dao.markForRecreate(row.id)
        } catch (e: WorkoutRemoteException.Conflict) {
            dao.markRejected(row.id, DUPLICATE_NAME)
        } catch (e: WorkoutRemoteException.Rejected) {
            dao.markRejected(row.id, e.message)
        }
    }

    private suspend fun pushDelete(id: String) {
        val row = current(id, SyncState.PENDING_DELETE) ?: return
        val serverId = row.serverId ?: return dao.deleteById(row.id)
        try {
            remote.delete(serverId)
            dao.deleteById(row.id)
        } catch (e: WorkoutRemoteException.NotFound) {
            dao.deleteById(row.id)
        } catch (e: WorkoutRemoteException.Rejected) {
            dao.markRejected(row.id, e.message)
        } catch (e: WorkoutRemoteException.Conflict) {
            dao.markRejected(row.id, DUPLICATE_NAME)
        }
    }
}
