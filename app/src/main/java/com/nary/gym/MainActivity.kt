package com.nary.gym

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.nary.gym.Notifications.NotificationHelper
import com.nary.gym.data.local.TokenManager
import com.nary.gym.navigation.AppNavigator
import com.nary.gym.navigation.SOURCE_REMINDER
import com.nary.gym.ui.theme.NaryToDoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
        handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)

        enableEdgeToEdge()
        val launchedFromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        currentIntent = if (savedInstanceState == null && !launchedFromHistory) intent else null
        handleNotificationIntent(intent)
        setContent {
            NaryToDoTheme {
                val navController = rememberNavController()
                AppNavigator(navController, tokenManager = tokenManager, currentIntent = currentIntent)
            }
        }
    }
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getStringExtra("source") == SOURCE_REMINDER) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.cancel(NotificationHelper.NOTIFICATION_ID)
        }
    }
}
