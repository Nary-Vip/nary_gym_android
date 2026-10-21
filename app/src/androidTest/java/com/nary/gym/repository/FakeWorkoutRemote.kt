package com.nary.gym.repository

import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.SyncState
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.repository.remote.WorkoutRemote
import com.nary.gym.repository.remote.WorkoutRemoteException

class FakeWorkoutRemote : WorkoutRemote {
    val server = mutableMapOf<String, WorkOuts>()
    val calls = mutableListOf<String>()
    val nextError = mutableMapOf<String, WorkoutRemoteException>()
    var duringCreate: (suspend () -> Unit)? = null
    var duringUpdate: (suspend () -> Unit)? = null
    private var nextId = 1

    private fun fail(op: String) {
        nextError.remove(op)?.let { throw it }
    }

    override suspend fun create(name: String, type: WorkOutType, imagePath: String): String {
        calls += "create:$name"
        fail("create")
        val id = "s${nextId++}"
        server[id] = WorkOuts(id, name, type, imagePath, serverId = id)
        duringCreate?.also { duringCreate = null }?.invoke()
        return id
    }

    override suspend fun update(serverId: String, name: String, type: WorkOutType, imagePath: String) {
        calls += "update:$serverId"
        fail("update")
        if (serverId !in server) throw WorkoutRemoteException.NotFound
        server[serverId] = WorkOuts(serverId, name, type, imagePath, serverId = serverId)
        duringUpdate?.also { duringUpdate = null }?.invoke()
    }

    override suspend fun delete(serverId: String) {
        calls += "delete:$serverId"
        fail("delete")
        if (server.remove(serverId) == null) throw WorkoutRemoteException.NotFound
    }

    override suspend fun findIdByExactName(name: String): String? {
        calls += "find:$name"
        return server.values.firstOrNull { it.workOutName == name }?.serverId
    }

    override suspend fun fetchAll(): List<WorkOuts> {
        calls += "fetchAll"
        fail("fetchAll")
        return server.values.map { it.copy(syncState = SyncState.SYNCED) }
    }
}
