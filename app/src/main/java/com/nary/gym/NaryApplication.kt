package com.nary.gym

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nary.gym.Notifications.AlarmScheduler
import com.nary.gym.Notifications.NotificationHelper
import com.nary.gym.Notifications.ReminderWatchdogWorker
import com.nary.gym.checkin.CheckInNotificationHelper
import com.nary.gym.checkin.GymGeofenceManager
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.local.TokenManager
import com.nary.gym.data.session.UserSession
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NaryApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var userSession: UserSession

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var gymGeofenceManager: GymGeofenceManager

    @Inject
    lateinit var gymLocationStore: GymLocationStore

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        CheckInNotificationHelper.createChannel(this)
        ReminderWatchdogWorker.ensureScheduled(this)

        applicationScope.launch {
            userSession.currentUser.collectLatest { user ->
                if (user == null && tokenManager.getAccessToken() == null) {
                    NotificationHelper.cancelNotification(this@NaryApplication)
                    AlarmScheduler.cancelDailyAlarm(this@NaryApplication)
                    gymLocationStore.setAutoCheckInEnabled(false)
                    gymGeofenceManager.unregister()
                } else if (user != null || tokenManager.getAccessToken() != null) {
                    if (tokenManager.getNotificationPreference()) {
                        AlarmScheduler.scheduleDailyAlarm(
                            this@NaryApplication,
                            tokenManager.getReminderHour(),
                            tokenManager.getReminderMinute()
                        )
                    }
                    gymGeofenceManager.syncRegistration()
                }
            }
        }
    }
}
