package com.nary.gym.data.room_db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.core.WorkOutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkOutsDaoTest {

    private lateinit var db: WorkoutDatabase
    private lateinit var dao: WorkOutsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java
        ).build()
        dao = db.workoutDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun workout(id: String, name: String = "Squat") =
        WorkOuts(id = id, workOutName = name, workOutType = WorkOutType.LEG, workOutImage = "/f/$id.jpg")

    private fun synced(id: String, name: String = "Squat") = workout(id, name).copy(serverId = id)

    @Test
    fun saveLocal_newRow_isPendingCreateWithVersion1() = runBlocking {
        dao.saveLocal(workout("local-1"))
        val row = dao.getById("local-1")!!
        assertEquals(SyncState.PENDING_CREATE, row.syncState)
        assertNull(row.serverId)
        assertEquals(1, row.version)
    }

    @Test
    fun saveLocal_onSyncedRow_becomesPendingUpdate_andBumpsVersion() = runBlocking {
        dao.mergeFromServer(listOf(synced("s1")))
        dao.saveLocal(workout("s1", "Front squat"))
        val row = dao.getById("s1")!!
        assertEquals(SyncState.PENDING_UPDATE, row.syncState)
        assertEquals("s1", row.serverId)
        assertEquals("Front squat", row.workOutName)
        assertEquals(1, row.version)
    }

    @Test
    fun saveLocal_onPendingCreate_staysPendingCreate() = runBlocking {
        dao.saveLocal(workout("local-1"))
        dao.saveLocal(workout("local-1", "Squat 2"))
        val row = dao.getById("local-1")!!
        assertEquals(SyncState.PENDING_CREATE, row.syncState)
        assertEquals("Squat 2", row.workOutName)
        assertEquals(2, row.version)
    }

    @Test
    fun saveLocal_clearsSyncError() = runBlocking {
        dao.saveLocal(workout("local-1"))
        dao.markRejected("local-1", "Name too short")
        dao.saveLocal(workout("local-1", "Squat fixed"))
        assertNull(dao.getById("local-1")!!.syncError)
    }

    @Test
    fun deleteLocal_neverSyncedRow_isRemoved() = runBlocking {
        dao.saveLocal(workout("local-1"))
        dao.deleteLocal("local-1")
        assertNull(dao.getById("local-1"))
    }

    @Test
    fun deleteLocal_syncedRow_becomesHiddenTombstone() = runBlocking {
        dao.mergeFromServer(listOf(synced("s1")))
        dao.deleteLocal("s1")
        assertEquals(emptyList<WorkOuts>(), dao.getAllWorkouts().first())
        assertEquals(SyncState.PENDING_DELETE, dao.getById("s1")!!.syncState)
    }

    @Test
    fun finishCreate_withChangedVersion_leavesPendingUpdate() = runBlocking {
        dao.saveLocal(workout("local-1"))
        val readVersion = dao.getById("local-1")!!.version
        dao.saveLocal(workout("local-1", "Edited while uploading"))
        assertEquals(1, dao.finishCreate("local-1", "s9", readVersion))
        val row = dao.getById("local-1")!!
        assertEquals("s9", row.serverId)
        assertEquals(SyncState.PENDING_UPDATE, row.syncState)
    }

    @Test
    fun finishCreate_onDeletedRow_returnsZero() = runBlocking {
        dao.saveLocal(workout("local-1"))
        val readVersion = dao.getById("local-1")!!.version
        dao.deleteLocal("local-1")
        assertEquals(0, dao.finishCreate("local-1", "s9", readVersion))
    }

    @Test
    fun merge_updatesAddsAndRemovesSyncedRows() = runBlocking {
        dao.mergeFromServer(listOf(synced("s1"), synced("s3", "Lunge")))
        dao.mergeFromServer(listOf(synced("s1", "Back squat"), synced("s4", "Deadlift")))
        assertEquals("Back squat", dao.getById("s1")!!.workOutName)
        assertNull(dao.getById("s3"))
        assertEquals(SyncState.SYNCED, dao.getById("s4")!!.syncState)
    }

    @Test
    fun merge_leavesPendingRowsUntouched() = runBlocking {
        dao.mergeFromServer(listOf(synced("s1"), synced("s2", "Lunge")))
        dao.saveLocal(workout("s1", "Local"))
        dao.deleteLocal("s2")
        dao.mergeFromServer(listOf(synced("s1", "Server"), synced("s2", "Server2")))
        val s1 = dao.getById("s1")!!
        assertEquals("Local", s1.workOutName)
        assertEquals(SyncState.PENDING_UPDATE, s1.syncState)
        assertEquals(SyncState.PENDING_DELETE, dao.getById("s2")!!.syncState)
    }

    @Test
    fun pendingForSync_skipsRejectedRows() = runBlocking {
        dao.saveLocal(workout("local-1"))
        dao.saveLocal(workout("local-2", "Lunge"))
        dao.markRejected("local-1", "Name too short")
        val pending = dao.pendingForSync().map { it.id }
        assertEquals(listOf("local-2"), pending)
        assertFalse("local-1" in pending)
    }
}
