package com.nary.gym.di

import com.nary.gym.sync.SessionCheck
import com.nary.gym.data.local.CurrentAccount
import com.nary.gym.data.local.TokenManager
import dagger.Provides
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
    abstract fun bindCurrentAccount(tokenManager: TokenManager): CurrentAccount

    @Binds
    abstract fun bindSyncRequester(scheduler: WorkoutSyncScheduler): SyncRequester

    companion object {
        @Provides
        fun provideSessionCheck(tokenManager: TokenManager): SessionCheck =
            SessionCheck { tokenManager.getAccessToken() != null }
    }
}
