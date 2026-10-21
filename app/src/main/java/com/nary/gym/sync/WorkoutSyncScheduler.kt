package com.nary.gym.sync

import javax.inject.Inject

fun interface SyncRequester {
    fun request()
}

class WorkoutSyncScheduler @Inject constructor() : SyncRequester {
    override fun request() = Unit
}
