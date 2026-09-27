package com.nary.gym.checkin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nary.gym.MainActivity
import com.nary.gym.R
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.utils.WorkoutCycle
import com.nary.gym.utils.formatDuration

object CheckInNotificationHelper {
    private const val CHANNEL_ID = "gym-checkin"
    private const val NOTIFICATION_ID = 7001
    const val ACTION_UNDO_CHECK_IN = "com.nary.gym.action.UNDO_CHECK_IN"
    const val EXTRA_VISIT_ID = "visit_id"
    const val SOURCE_CHECK_IN = "checkin"
    
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gym check-in",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Automatic check-in and check-out at your gym"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showCheckedIn(context: Context, visit: GymVisit) {
        val today = WorkoutCycle(context).getCurrentDayWorkout().title
        val undoIntent = Intent(context, CheckInActionReceiver::class.java).apply {
            action = ACTION_UNDO_CHECK_IN
            putExtra(EXTRA_VISIT_ID, visit.id)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context, 2, undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notify(
            context,
            baseBuilder(context)
                .setContentTitle("Checked in at the gym")
                .setContentText("Today: $today day")
                .addAction(0, "Undo", undoPendingIntent)
        )
    }

    fun showCheckedOut(context: Context, visit: GymVisit) {
        val duration = formatDuration((visit.checkOutAt ?: visit.checkInAt) - visit.checkInAt)
        notify(
            context,
            baseBuilder(context)
                .setContentTitle("Checked out")
                .setContentText("Workout time: $duration")
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun baseBuilder(context: Context): NotificationCompat.Builder {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("source", SOURCE_CHECK_IN)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 124, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
    }

    private fun notify(context: Context, builder: NotificationCompat.Builder) {
        // Silently no-ops if POST_NOTIFICATIONS was denied; the visit is still recorded.
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build())
    }
}
