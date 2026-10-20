package com.nary.gym.Notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.time.ZoneId

object AlarmScheduler {
    private const val DAILY_REQUEST_CODE = 0
    private const val SNOOZE_REQUEST_CODE = 2

    fun nextDailyTrigger(nowMillis: Long, zone: ZoneId, hour: Int, minute: Int): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val today = now.toLocalDate().atTime(hour, minute).atZone(zone)
        val next = if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(zone)
        return next.toInstant().toEpochMilli()
    }

    fun snoozeAlarm(context: Context){
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(context, SNOOZE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze = System.currentTimeMillis() + (30 * 60 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snooze, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snooze, pendingIntent)
        }
    }

    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int){
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(context, DAILY_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val triggerAt = nextDailyTrigger(System.currentTimeMillis(), ZoneId.systemDefault(), hour, minute)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        listOf(DAILY_REQUEST_CODE, SNOOZE_REQUEST_CODE).forEach { requestCode ->
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return@forEach
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}