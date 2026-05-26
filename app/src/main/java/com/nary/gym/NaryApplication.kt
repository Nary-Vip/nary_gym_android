package com.nary.gym

import android.app.Application
import com.nary.gym.Notifications.NotificationHelper
import com.nary.gym.data.session.UserSession
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NaryApplication : Application() {

    @Inject
    lateinit var userSession: UserSession

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            userSession.currentUser.collectLatest { user ->
                if (user == null) {
                    NotificationHelper.cancelNotification(this@NaryApplication)
                }
            }
        }
    }
}
