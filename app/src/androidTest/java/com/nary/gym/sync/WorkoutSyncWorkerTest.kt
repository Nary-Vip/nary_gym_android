package com.nary.gym.sync

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.data.room_db.WorkoutDatabase
import com.nary.gym.repository.FakeAccount
import com.nary.gym.repository.FakeWorkoutRemote
import com.nary.gym.repository.remote.WorkOutRepository
import com.nary.gym.repository.remote.WorkoutRemoteException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSyncWorkerTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: WorkoutDatabase
    private val remote = FakeWorkoutRemote()
    private lateinit var repository: WorkOutRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        repository = WorkOutRepository(db.workoutDao(), remote, FakeAccount("u1")) {}
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun worker(loggedIn: Boolean): WorkoutSyncWorker =
        TestListenableWorkerBuilder<WorkoutSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = WorkoutSyncWorker(appContext, workerParameters, { loggedIn }, repository)
            })
            .build()

    private suspend fun pending() = repository.save(
        WorkOuts(id = "", workOutName = "Squat", workOutType = WorkOutType.LEG, workOutImage = "/f/a.jpg")
    )

    @Test
    fun withoutToken_succeedsWithoutSyncing() = runBlocking {
        pending()
        assertEquals(ListenableWorker.Result.success(), worker(loggedIn = false).doWork())
        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun retryableSync_returnsRetry() = runBlocking {
        pending()
        remote.nextError["create"] = WorkoutRemoteException.Retryable(null)
        assertEquals(ListenableWorker.Result.retry(), worker(loggedIn = true).doWork())
    }

    @Test
    fun successfulSync_returnsSuccess() = runBlocking {
        pending()
        assertEquals(ListenableWorker.Result.success(), worker(loggedIn = true).doWork())
        assertTrue("create:Squat" in remote.calls)
    }

    @Test
    fun request_enqueuesUniqueNetworkConstrainedWork() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        val scheduler = WorkoutSyncScheduler(context)
        try {
            scheduler.request()
            val infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WorkoutSyncScheduler.UNIQUE_WORK_NAME).get()
            assertTrue(infos.isNotEmpty())
            assertEquals(NetworkType.CONNECTED, infos.first().constraints.requiredNetworkType)
        } finally {
            scheduler.cancel()
        }
    }
}
