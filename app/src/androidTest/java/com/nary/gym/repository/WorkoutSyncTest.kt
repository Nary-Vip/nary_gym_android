package com.nary.gym.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.SyncState
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.data.room_db.WorkOutsDao
import com.nary.gym.data.room_db.WorkoutDatabase
import com.nary.gym.repository.remote.SyncOutcome
import com.nary.gym.repository.remote.WorkOutRepository
import com.nary.gym.repository.remote.WorkoutRemoteException
import com.nary.gym.sync.SyncRequester
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSyncTest {

    private lateinit var db: WorkoutDatabase
    private lateinit var dao: WorkOutsDao
    private val remote = FakeWorkoutRemote()
    private val account = FakeAccount("u1")
    private var requests = 0
    private lateinit var repository: WorkOutRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WorkoutDatabase::class.java).build()
        dao = db.workoutDao()
        repository = WorkOutRepository(dao, remote, account, SyncRequester { requests++ })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val squat = WorkOuts(id = "", workOutName = "Squat", workOutType = WorkOutType.LEG, workOutImage = "/f/a.jpg")

    private suspend fun only(): WorkOuts = repository.getAllWorkouts().first().single()

    private suspend fun serverRow(id: String, name: String) {
        remote.server[id] = squat.copy(id = id, workOutName = name, serverId = id)
    }

    @Test
    fun save_isInstantAndRequestsSync() = runBlocking {
        repository.save(squat)
        val row = only()
        assertTrue(row.id.startsWith("local-"))
        assertEquals(SyncState.PENDING_CREATE, row.syncState)
        assertEquals(1, requests)
        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun createdOffline_isUploadedAndMarkedSynced() = runBlocking {
        repository.save(squat)
        assertEquals(SyncOutcome.SUCCESS, repository.sync())
        val row = only()
        assertEquals("s1", row.serverId)
        assertEquals(SyncState.SYNCED, row.syncState)
        assertTrue(row.id.startsWith("local-"))
        assertEquals("Squat", remote.server["s1"]!!.workOutName)
    }

    @Test
    fun editDuringCreate_isUploadedOnTheNextPass() = runBlocking {
        repository.save(squat)
        val row = only()
        remote.duringCreate = { repository.save(row.copy(workOutName = "Squat 2")) }
        repository.sync()
        val afterFirst = dao.getById(row.id)!!
        assertEquals(SyncState.PENDING_UPDATE, afterFirst.syncState)
        assertNotNull(afterFirst.serverId)
        repository.sync()
        assertEquals(SyncState.SYNCED, dao.getById(row.id)!!.syncState)
        assertEquals("Squat 2", remote.server["s1"]!!.workOutName)
    }

    @Test
    fun editDuringUpdate_staysPending() = runBlocking {
        serverRow("s5", "Squat")
        repository.sync()
        repository.save(only().copy(workOutName = "Squat A"))
        remote.duringUpdate = { repository.save(only().copy(workOutName = "Squat B")) }
        repository.sync()
        val row = dao.getById("s5")!!
        assertEquals(SyncState.PENDING_UPDATE, row.syncState)
        assertEquals("Squat B", row.workOutName)
        repository.sync()
        assertEquals("Squat B", remote.server["s5"]!!.workOutName)
        assertEquals(SyncState.SYNCED, dao.getById("s5")!!.syncState)
    }

    @Test
    fun deleteDuringCreate_deletesTheNewServerCopy() = runBlocking {
        repository.save(squat)
        val row = only()
        remote.duringCreate = { repository.delete(row) }
        repository.sync()
        assertTrue("delete:s1" in remote.calls)
        assertTrue(remote.server.isEmpty())
        assertTrue(repository.getAllWorkouts().first().isEmpty())
        assertNull(dao.getById(row.id))
    }

    @Test
    fun updateNotFound_isRecreatedOnTheNextSync() = runBlocking {
        serverRow("s5", "Squat")
        repository.sync()
        remote.server.clear()
        repository.save(only().copy(workOutName = "Squat edited"))
        repository.sync()
        repository.sync()
        val row = dao.getById("s5")!!
        assertEquals(SyncState.SYNCED, row.syncState)
        assertEquals("Squat edited", remote.server[row.serverId]!!.workOutName)
    }

    @Test
    fun createConflict_adoptsTheExistingServerWorkout() = runBlocking {
        serverRow("s7", "Squat")
        repository.save(squat.copy(id = "local-x"))
        remote.nextError["create"] = WorkoutRemoteException.Conflict
        repository.sync()
        assertEquals("update:s7", remote.calls.last { it.startsWith("update") })
        val row = dao.getById("local-x")!!
        assertEquals("s7", row.serverId)
        assertEquals(SyncState.SYNCED, row.syncState)
        assertEquals(1, repository.getAllWorkouts().first().size)
    }

    @Test
    fun rejected_setsSyncError_andIsSkippedUntilEdited() = runBlocking {
        repository.save(squat)
        remote.nextError["create"] = WorkoutRemoteException.Rejected("Name too short")
        repository.sync()
        val row = only()
        assertEquals("Name too short", row.syncError)
        remote.calls.clear()
        repository.sync()
        assertFalse(remote.calls.any { it.startsWith("create") })
        repository.save(row.copy(workOutName = "Squat fixed"))
        assertNull(dao.getById(row.id)!!.syncError)
        repository.sync()
        assertEquals(SyncState.SYNCED, dao.getById(row.id)!!.syncState)
    }

    @Test
    fun deleteNotFound_removesTheTombstone() = runBlocking {
        serverRow("s5", "Squat")
        repository.sync()
        repository.delete(only())
        remote.server.clear()
        assertEquals(SyncOutcome.SUCCESS, repository.sync())
        assertNull(dao.getById("s5"))
    }

    @Test
    fun retryableFailure_stopsAndKeepsEverythingPending() = runBlocking {
        repository.save(squat)
        remote.nextError["create"] = WorkoutRemoteException.Retryable(null)
        assertEquals(SyncOutcome.RETRY, repository.sync())
        assertFalse("fetchAll" in remote.calls)
        assertEquals(SyncState.PENDING_CREATE, only().syncState)
    }

    @Test
    fun save_stampsTheCurrentAccount() = runBlocking {
        repository.save(squat)
        assertEquals("u1", only().ownerId)
    }

    @Test
    fun otherAccountsPendingChange_isNotUploaded_untilThatAccountReturns() = runBlocking {
        repository.save(squat)
        account.accountId.value = "u2"
        assertEquals(SyncOutcome.SUCCESS, repository.sync())
        assertFalse(remote.calls.any { it.startsWith("create") })
        assertTrue(repository.getAllWorkouts().first().isEmpty())
        account.accountId.value = "u1"
        assertEquals(SyncState.PENDING_CREATE, only().syncState)
        repository.sync()
        assertTrue("create:Squat" in remote.calls)
        assertEquals(SyncState.SYNCED, only().syncState)
    }

    @Test
    fun noAccountId_retriesWithoutCalls_thenClaimsTheRow() = runBlocking {
        account.accountId.value = null
        repository.save(squat)
        assertEquals(1, repository.getAllWorkouts().first().size)
        assertEquals(SyncOutcome.RETRY, repository.sync())
        assertTrue(remote.calls.isEmpty())
        account.accountId.value = "u1"
        assertEquals(SyncOutcome.SUCCESS, repository.sync())
        assertEquals("u1", only().ownerId)
        assertEquals(SyncState.SYNCED, only().syncState)
    }

    @Test
    fun accountChangeDuringSync_stopsBeforeTheNextUpload() = runBlocking {
        repository.save(squat)
        repository.save(squat.copy(workOutName = "Lunge"))
        remote.duringCreate = { account.accountId.value = "u2" }
        assertEquals(SyncOutcome.RETRY, repository.sync())
        assertEquals(1, remote.calls.count { it.startsWith("create") })
        assertFalse("fetchAll" in remote.calls)
    }
}
