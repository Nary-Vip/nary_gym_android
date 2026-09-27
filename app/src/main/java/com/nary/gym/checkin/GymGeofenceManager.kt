package com.nary.gym.checkin

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.nary.gym.data.local.GymLocation
import com.nary.gym.data.local.GymLocationStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymGeofenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: GymLocationStore,
) {
    private val client = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // Must be MUTABLE: Play services writes the GeofencingEvent into this intent's extras.
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /** Registers the gym geofence if enabled, set and permitted; otherwise removes it. Returns true if active. */
    @SuppressLint("MissingPermission") // checked via currentLocationPermissionStep()
    suspend fun syncRegistration(): Boolean {
        val gym = store.getGym()
        if (!store.isAutoCheckInEnabled() || gym == null ||
            context.currentLocationPermissionStep() != LocationPermissionStep.GRANTED
        ) {
            unregister()
            return false
        }
        return try {
            // Adding with the same request id replaces the existing geofence.
            client.addGeofences(buildRequest(gym), pendingIntent).await()
            true
        } catch (e: Exception) {
            // Typically GEOFENCE_NOT_AVAILABLE because device location is off.
            Log.e(TAG, "Failed to register gym geofence", e)
            false
        }
    }

    suspend fun unregister() {
        try {
            client.removeGeofences(listOf(GEOFENCE_ID)).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove gym geofence", e)
        }
    }

    private fun buildRequest(gym: GymLocation): GeofencingRequest {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(gym.latitude, gym.longitude, gym.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setLoiteringDelay(LOITERING_DELAY_MS)
            // Let the system batch location checks to save battery; check-in isn't time-critical.
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .build()
        return GeofencingRequest.Builder()
            // If already inside when registering, deliver DWELL after the loitering delay.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(geofence)
            .build()
    }

    private companion object {
        const val TAG = "GymGeofenceManager"
        const val GEOFENCE_ID = "nary_gym"
        const val REQUEST_CODE = 4201
        val LOITERING_DELAY_MS = Duration.ofMinutes(3).toMillis().toInt()
        val RESPONSIVENESS_MS = Duration.ofMinutes(2).toMillis().toInt()
    }
}
