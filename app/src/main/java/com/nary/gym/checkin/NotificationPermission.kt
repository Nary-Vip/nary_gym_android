package com.nary.gym.checkin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

private const val API_TIRAMISU = 33 // POST_NOTIFICATIONS became a runtime permission in Android 13

/**
 * Whether POST_NOTIFICATIONS still has to be requested. Without it Android silently drops the
 * check-in/out notifications - and with them the only Undo for a false check-in.
 */
fun needsNotificationPermission(sdkInt: Int, granted: Boolean): Boolean =
    sdkInt >= API_TIRAMISU && !granted

fun Context.needsNotificationPermission(): Boolean = needsNotificationPermission(
    sdkInt = Build.VERSION.SDK_INT,
    granted = Build.VERSION.SDK_INT < API_TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED,
)
