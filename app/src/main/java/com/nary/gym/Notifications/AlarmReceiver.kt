package com.nary.gym.Notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nary.gym.data.local.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onReceive(context: Context, p1: Intent?) {
        val remindersOn = runCatching { tokenManager.getNotificationPreference() }
            .onFailure { Log.e("AlarmReceiver", "Failed to read reminder preference", it) }
            .getOrDefault(true)
        if (!remindersOn) return
        NotificationHelper.showGym(context)
        try {
            // This self-reschedule is the only thing that arms tomorrow's alarm; if reading
            // prefs throws (e.g. an EncryptedSharedPreferences edge case) the chain must not
            // die silently here - ReminderWatchdogWorker is the backstop for that.
            if (tokenManager.getNotificationPreference()) {
                AlarmScheduler.scheduleDailyAlarm(
                    context,
                    tokenManager.getReminderHour(),
                    tokenManager.getReminderMinute()
                )
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to reschedule daily alarm", e)
        }
    }
}