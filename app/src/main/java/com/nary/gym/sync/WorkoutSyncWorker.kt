package com.nary.gym.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nary.gym.repository.remote.SyncOutcome
import com.nary.gym.repository.remote.WorkOutRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

fun interface SessionCheck {
    fun isLoggedIn(): Boolean
}

@HiltWorker
class WorkoutSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val session: SessionCheck,
    private val repository: WorkOutRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!session.isLoggedIn()) return Result.success()
        return when (repository.sync()) {
            SyncOutcome.SUCCESS -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
        }
    }
}
