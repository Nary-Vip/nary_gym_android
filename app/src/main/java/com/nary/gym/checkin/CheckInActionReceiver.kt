package com.nary.gym.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nary.gym.repository.local.GymVisitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Undo" on the check-in notification: the geofence fired but the user isn't really at the gym. */
@AndroidEntryPoint
class CheckInActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: GymVisitRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CheckInNotificationHelper.ACTION_UNDO_CHECK_IN) return
        val visitId = intent.getStringExtra(CheckInNotificationHelper.EXTRA_VISIT_ID) ?: return
        CheckInNotificationHelper.cancel(context)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.delete(visitId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to undo check-in $visitId", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "CheckInActionReceiver"
    }
}
