package com.nary.gym.Notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nary.gym.MainActivity
import com.nary.gym.R

object NotificationHelper {
    const val NOTIFICATION_CHANNEL = "nary-gym"
    const val NOTIFICATION_NAME = "GYM Reminder"
    const val NOTIFICATION_DESC = "Start your daily workout, Get Fit and Healthy"
    const val NOTIFICATION_ID = 6969
    const val ACTION_YES = "action_yes"
    const val ACTION_SNOOZE = "action_snooze"

    fun createNotificationChannel(context: Context){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            NOTIFICATION_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = NOTIFICATION_DESC
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showGym(context: Context){
        val manager = context.getSystemService(NotificationManager::class.java)

        val yesIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("source", "notification_yes")
        }

        val yesPendingIntent = PendingIntent.getActivity(
            context,
            123,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(context, 1, snoozeIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle("Get Ready for the GYM")
            .setContentText(NOTIFICATION_DESC)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .addAction(0, "Yes", yesPendingIntent)
            .addAction(0, "Snooze", snoozePendingIntent)

        manager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
}