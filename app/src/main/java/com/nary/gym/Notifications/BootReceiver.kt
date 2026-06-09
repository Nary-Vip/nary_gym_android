package com.nary.gym.Notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nary.gym.data.local.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver: BroadcastReceiver() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED){
            if (tokenManager.getNotificationPreference()) {
                AlarmScheduler.scheduleDailyAlarm(
                    context,
                    tokenManager.getReminderHour(),
                    tokenManager.getReminderMinute()
                )
            }
        }
    }
}