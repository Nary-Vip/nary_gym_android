package com.nary.gym.Notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, p1: Intent?) {
        NotificationHelper.showGym(context)
        AlarmScheduler.scheduleDailyAlarm(context)
    }
}