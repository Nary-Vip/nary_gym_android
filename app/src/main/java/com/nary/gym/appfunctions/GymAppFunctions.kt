package com.nary.gym.appfunctions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import androidx.core.content.ContextCompat
import com.nary.gym.Notifications.AlarmScheduler
import com.nary.gym.data.local.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * AppFunctions for Nary Gym to allow system-level interactions.
 */
@RequiresApi(36)
class GymAppFunctions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager
) {
    /**
     * Schedules a daily workout reminder at the specified time.
     */
    @AppFunction
    fun scheduleGymReminder(
        appFunctionContext: AppFunctionContext,
        hour: Int,
        minute: Int
    ): GymReminderResponse {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return GymReminderResponse(
                "Notifications aren't allowed for Nary Gym yet. Open the app and grant notification " +
                    "permission to receive your gym reminder."
            )
        }

        tokenManager.saveReminderTime(hour, minute)
        tokenManager.saveNotificationPreference(true)
        AlarmScheduler.scheduleDailyAlarm(context, hour, minute)
        return GymReminderResponse("Gym reminder scheduled for ${String.format("%02d:%02d", hour, minute)}")
    }
}

/**
 * Response for gym reminder scheduling.
 */
@AppFunctionSerializable
data class GymReminderResponse(val message: String)
