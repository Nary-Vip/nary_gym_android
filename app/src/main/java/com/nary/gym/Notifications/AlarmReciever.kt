package com.nary.gym.Notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nary.gym.data.local.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onReceive(context: Context, p1: Intent?) {
        NotificationHelper.showGym(context)
        if (tokenManager.getNotificationPreference()) {
            AlarmScheduler.scheduleDailyAlarm(
                context,
                tokenManager.getReminderHour(),
                tokenManager.getReminderMinute()
            )
        }
    }
}