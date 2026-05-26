package com.nary.gym.Notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)

        when(intent.action){
            NotificationHelper.ACTION_SNOOZE -> {
                manager.cancel(NotificationHelper.NOTIFICATION_ID)
                AlarmScheduler.snoozeAlarm(context)
            }
        }
    }
}