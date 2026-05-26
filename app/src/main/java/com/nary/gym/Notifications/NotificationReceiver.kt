package com.nary.gym.Notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.cancel(NotificationHelper.NOTIFICATION_ID)

        when(intent.action){
            NotificationHelper.ACTION_SNOOZE -> {
                AlarmScheduler.snoozeAlarm(context)
            }
        }


    }
}