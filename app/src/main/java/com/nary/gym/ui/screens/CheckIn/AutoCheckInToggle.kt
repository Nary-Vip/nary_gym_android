package com.nary.gym.ui.screens.CheckIn

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nary.gym.checkin.LocationPermissionStep
import com.nary.gym.checkin.currentLocationPermissionStep
import com.nary.gym.ui.theme.Accent
import com.nary.gym.ui.theme.LabelColor
import com.nary.gym.ui.theme.TextColor

/**
 * Auto check-in switch plus the permission ladder: fine+coarse → (API 29+) background.
 * On API 30+ the background request opens the system Settings page, so the step is
 * re-read on every resume to notice the user coming back.
 */
@Composable
fun AutoCheckInToggle(
    enabled: Boolean,
    gymIsSet: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(context.currentLocationPermissionStep()) }
    var pendingEnable by remember { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        step = context.currentLocationPermissionStep()
        onPauseOrDispose { }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { step = context.currentLocationPermissionStep() }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        step = context.currentLocationPermissionStep()
        if (step == LocationPermissionStep.REQUEST_BACKGROUND) showBackgroundRationale = true
    }

    // Finish enabling once every permission is in place, possibly after returning from Settings.
    LaunchedEffect(step, pendingEnable) {
        if (pendingEnable && step == LocationPermissionStep.GRANTED) {
            pendingEnable = false
            onEnable()
        }
    }

    fun advance() {
        pendingEnable = true
        when (step) {
            LocationPermissionStep.REQUEST_FOREGROUND, LocationPermissionStep.NEED_PRECISE ->
                foregroundLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            LocationPermissionStep.REQUEST_BACKGROUND -> showBackgroundRationale = true
            LocationPermissionStep.GRANTED -> Unit // LaunchedEffect above calls onEnable
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto check-in", color = TextColor, fontSize = 16.sp)
                Text(
                    if (gymIsSet) "Check in when you arrive, out when you leave" else "Set your gym location first",
                    color = LabelColor, fontSize = 13.sp
                )
            }
            Switch(
                checked = enabled,
                enabled = gymIsSet,
                onCheckedChange = { checked ->
                    if (checked) advance() else {
                        pendingEnable = false
                        onDisable()
                    }
                }
            )
        }

        if (enabled && step != LocationPermissionStep.GRANTED) {
            Text("Auto check-in is paused: location permission is missing.", color = Accent, fontSize = 13.sp)
            TextButton(onClick = { advance() }) { Text("Fix permissions") }
        }
        if (step == LocationPermissionStep.NEED_PRECISE) {
            Text("Auto check-in needs Precise location, not Approximate.", color = LabelColor, fontSize = 13.sp)
            TextButton(onClick = { openAppSettings(context) }) { Text("Open app settings") }
        }
    }

    if (showBackgroundRationale) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundRationale = false
                pendingEnable = false
            },
            title = { Text("Allow location all the time") },
            text = {
                Text(
                    "To check you in while the app is closed, choose \"${backgroundOptionLabel(context)}\" " +
                        "on the next screen. Your location is only compared with your gym and never leaves this phone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundRationale = false
                    // Only reachable on API 29+ (REQUEST_BACKGROUND is never returned below that).
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackgroundRationale = false
                    pendingEnable = false
                }) { Text("Not now") }
            }
        )
    }
}

private fun backgroundOptionLabel(context: Context): CharSequence =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.backgroundPermissionOptionLabel
    } else {
        "Allow all the time"
    }

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
