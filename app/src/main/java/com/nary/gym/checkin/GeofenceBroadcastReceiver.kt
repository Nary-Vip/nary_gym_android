package com.nary.gym.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.repository.local.CheckInResult
import com.nary.gym.repository.local.CheckOutResult
import com.nary.gym.repository.local.GymVisitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: GymVisitRepository

    @Inject
    lateinit var store: GymLocationStore

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "Geofence error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        if (!store.isAutoCheckInEnabled()) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (event.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_DWELL -> {
                        val result = repository.checkIn(VisitSource.AUTO)
                        if (result is CheckInResult.CheckedIn) {
                            CheckInNotificationHelper.showCheckedIn(context, result.visit)
                        }
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        val result = repository.checkOut()
                        if (result is CheckOutResult.CheckedOut) {
                            CheckInNotificationHelper.showCheckedOut(context, result.visit)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle geofence transition", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
