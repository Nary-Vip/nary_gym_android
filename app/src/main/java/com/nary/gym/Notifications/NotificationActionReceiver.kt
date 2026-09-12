package com.nary.gym.Notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nary.gym.MainActivity

class NotificationActionReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)

        when(intent.action){
            NotificationHelper.ACTION_YES -> {
                manager.cancel(NotificationHelper.NOTIFICATION_ID)
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("source", "notification_yes")
                }
                context.startActivity(mainIntent)
            }
            NotificationHelper.ACTION_SNOOZE -> {
                manager.cancel(NotificationHelper.NOTIFICATION_ID)
                AlarmScheduler.snoozeAlarm(context)
            }
        }
    }
}