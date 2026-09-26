package com.nary.gym.checkin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Where the user is in the permission ladder auto check-in needs. */
enum class LocationPermissionStep { REQUEST_FOREGROUND, NEED_PRECISE, REQUEST_BACKGROUND, GRANTED }

private const val API_Q = 29 // ACCESS_BACKGROUND_LOCATION was introduced in Android 10

/**
 * Geofencing needs precise (fine) location, and to fire while the app is closed it also needs
 * background location on API 29+. Kept pure so the ladder can be unit-tested.
 */
fun nextLocationPermissionStep(
    sdkInt: Int,
    hasFine: Boolean,
    hasCoarse: Boolean,
    hasBackground: Boolean,
): LocationPermissionStep = when {
    !hasFine && !hasCoarse -> LocationPermissionStep.REQUEST_FOREGROUND
    !hasFine -> LocationPermissionStep.NEED_PRECISE
    sdkInt >= API_Q && !hasBackground -> LocationPermissionStep.REQUEST_BACKGROUND
    else -> LocationPermissionStep.GRANTED
}

fun Context.hasFineLocationPermission(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

fun Context.currentLocationPermissionStep(): LocationPermissionStep = nextLocationPermissionStep(
    sdkInt = Build.VERSION.SDK_INT,
    hasFine = hasFineLocationPermission(),
    hasCoarse = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION),
    hasBackground = Build.VERSION.SDK_INT < API_Q ||
        isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
)

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
