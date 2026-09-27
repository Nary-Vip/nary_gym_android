package com.nary.gym.checkin

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentLocationProvider @Inject constructor(@ApplicationContext context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getCurrentLocation(): Location? {
        val cancellation = CancellationTokenSource()
        return client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .await(cancellation)
    }
}
