package com.nary.gym.Notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.nary.gym.MainActivity
import com.nary.gym.checkin.GymGeofenceManager
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.local.TokenManager
import com.nary.gym.navigation.SOURCE_REMINDER
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSchedulingTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val tokenManager = TokenManager(context)
    private var savedEnabled = false
    private var savedHour = 0
    private var savedMinute = 0

    @Before
    fun saveUserSettings() {
        savedEnabled = tokenManager.getNotificationPreference()
        savedHour = tokenManager.getReminderHour()
        savedMinute = tokenManager.getReminderMinute()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        disarm(DAILY)
        disarm(SNOOZE)
        NotificationHelper.cancelNotification(context)
    }

    @After
    fun restoreUserSettings() {
        disarm(DAILY)
        disarm(SNOOZE)
        NotificationHelper.cancelNotification(context)
        tokenManager.saveNotificationPreference(savedEnabled)
        tokenManager.saveReminderTime(savedHour, savedMinute)
        if (savedEnabled) AlarmScheduler.scheduleDailyAlarm(context, savedHour, savedMinute)
    }

    @Test
    fun scheduling_armsTheDailyAlarm() {
        AlarmScheduler.scheduleDailyAlarm(context, 7, 30)
        assertTrue(armed(DAILY))
    }

    @Test
    fun cancelling_disarmsTheDailyAlarmAndAPendingSnooze() {
        AlarmScheduler.scheduleDailyAlarm(context, 7, 30)
        AlarmScheduler.snoozeAlarm(context)
        AlarmScheduler.cancelDailyAlarm(context)
        assertFalse(armed(DAILY))
        assertFalse(armed(SNOOZE))
    }

    @Test
    fun alarmFiring_withRemindersOn_showsTheReminderAndArmsTomorrow() {
        tokenManager.saveNotificationPreference(true)
        tokenManager.saveReminderTime(7, 30)
        AlarmReceiver().onReceive(context, Intent(context, AlarmReceiver::class.java))
        val reminder = activeReminder()
        assertEquals(listOf("Yes", "Snooze"), reminder?.notification?.actions?.map { it.title.toString() })
        assertTrue(armed(DAILY))
    }

    @Test
    fun alarmFiring_withRemindersOff_showsNothing() {
        tokenManager.saveNotificationPreference(false)
        AlarmReceiver().onReceive(context, Intent(context, AlarmReceiver::class.java))
        assertEquals(null, activeReminder())
        assertFalse(armed(DAILY))
    }

    @Test
    fun snoozeAction_clearsTheReminderAndArmsASnooze() {
        NotificationHelper.showGym(context)
        NotificationActionReceiver().onReceive(context, Intent(NotificationHelper.ACTION_SNOOZE))
        assertEquals(null, activeReminder())
        assertTrue(armed(SNOOZE))
    }

    @Test
    fun openingTheAppFromYes_clearsTheReminder() {
        NotificationHelper.showGym(context)
        val intent = Intent(context, MainActivity::class.java).putExtra("source", SOURCE_REMINDER)
        ActivityScenario.launch<MainActivity>(intent).use {
            assertEquals(null, activeReminder())
        }
    }

    @Test
    fun watchdog_withRemindersOn_rearmsTheDailyAlarm() {
        tokenManager.saveNotificationPreference(true)
        tokenManager.saveReminderTime(7, 30)
        runBlocking { watchdog().doWork() }
        assertTrue(armed(DAILY))
    }

    @Test
    fun watchdog_withRemindersOff_leavesItDisarmed() {
        tokenManager.saveNotificationPreference(false)
        runBlocking { watchdog().doWork() }
        assertFalse(armed(DAILY))
    }

    private fun watchdog(): ReminderWatchdogWorker =
        TestListenableWorkerBuilder<ReminderWatchdogWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = ReminderWatchdogWorker(
                    appContext,
                    workerParameters,
                    tokenManager,
                    GymGeofenceManager(appContext, GymLocationStore(appContext)),
                )
            })
            .build()

    private fun activeReminder() = context.getSystemService(NotificationManager::class.java)
        .activeNotifications.firstOrNull { it.id == NotificationHelper.NOTIFICATION_ID }

    private fun alarmIntent(requestCode: Int, flags: Int) = PendingIntent.getBroadcast(
        context, requestCode, Intent(context, AlarmReceiver::class.java), flags or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun armed(requestCode: Int) = alarmIntent(requestCode, PendingIntent.FLAG_NO_CREATE) != null

    private fun disarm(requestCode: Int) {
        val pendingIntent = alarmIntent(requestCode, PendingIntent.FLAG_NO_CREATE) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private companion object {
        const val DAILY = 0
        const val SNOOZE = 2
    }
}
