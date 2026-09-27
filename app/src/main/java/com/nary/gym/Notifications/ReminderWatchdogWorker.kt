package com.nary.gym.Notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nary.gym.checkin.GymGeofenceManager
import com.nary.gym.data.local.TokenManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Backstop for the daily reminder alarm. AlarmReceiver normally reschedules itself each time it
 * fires, but that chain has a single point of failure: if the OS/OEM battery manager silently
 * drops one firing (or the app is force-stopped, which cancels all its pending AlarmManager
 * entries), every future reminder stops with no recovery until the user happens to reboot the
 * phone or reopen the app. This worker runs independently of that chain and just re-arms the
 * alarm - idempotent, since AlarmScheduler always updates the same PendingIntent rather than
 * creating a new one. It also re-syncs the gym check-in geofence, which the system drops when
 * device location is switched off and nothing else restores until the next process start.
 */
@HiltWorker
class ReminderWatchdogWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val tokenManager: TokenManager,
    private val gymGeofenceManager: GymGeofenceManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (tokenManager.getNotificationPreference()) {
            AlarmScheduler.scheduleDailyAlarm(
                applicationContext,
                tokenManager.getReminderHour(),
                tokenManager.getReminderMinute()
            )
        }
        // Also the backstop for the gym geofence (dropped when location is toggled off).
        gymGeofenceManager.syncRegistration()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "reminder_watchdog"

        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWatchdogWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
