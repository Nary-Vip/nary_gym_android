package com.nary.gym.di

import android.content.Context
import com.nary.gym.utils.WorkoutCycle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkoutCycleModule {
    @Provides
    @Singleton
    fun provideWorkoutCycle(@ApplicationContext context: Context): WorkoutCycle = WorkoutCycle(context)
}
