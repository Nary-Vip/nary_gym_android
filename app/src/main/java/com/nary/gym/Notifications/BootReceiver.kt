package com.nary.gym.Notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nary.gym.checkin.GymGeofenceManager
import com.nary.gym.data.local.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver: BroadcastReceiver() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var gymGeofenceManager: GymGeofenceManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED){
            if (tokenManager.getNotificationPreference()) {
                AlarmScheduler.scheduleDailyAlarm(
                    context,
                    tokenManager.getReminderHour(),
                    tokenManager.getReminderMinute()
                )
            }

            // Geofences are wiped on reboot. Re-register here rather than relying only on
            // NaryApplication: goAsync keeps this receiver (and so the process) alive until
            // Play services confirms, instead of racing a process kill after onReceive returns.
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    gymGeofenceManager.syncRegistration()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to restore gym geofence", e)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}