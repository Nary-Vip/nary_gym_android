package com.nary.gym.di

import com.nary.gym.repository.remote.RetrofitWorkoutRemote
import com.nary.gym.repository.remote.WorkoutRemote
import com.nary.gym.sync.SyncRequester
import com.nary.gym.sync.WorkoutSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutSyncModule {
    @Binds
    abstract fun bindWorkoutRemote(remote: RetrofitWorkoutRemote): WorkoutRemote

    @Binds
    abstract fun bindSyncRequester(scheduler: WorkoutSyncScheduler): SyncRequester
}
