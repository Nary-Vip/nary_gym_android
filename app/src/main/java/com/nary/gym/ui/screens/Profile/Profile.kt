package com.nary.gym.ui.screens.Profile

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nary.gym.Notifications.AlarmScheduler
import com.nary.gym.data.remote.dto.UserResponse
import com.nary.gym.ui.components.NaryTextField
import com.nary.gym.ui.components.SectionHeader
import com.nary.gym.viewModel.ProfileUiState
import com.nary.gym.viewModel.ProfileViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

const val LOGOUT_BUTTON_TAG = "logout_button"
const val CONFIRM_LOGOUT_TAG = "confirm_logout"

private fun reminderLabel(hour: Int, minute: Int): String =
    LocalTime.of(hour, minute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState = profileViewModel.uiState
    val user by profileViewModel.currentUser.collectAsState()
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        user?.let {
            name = it.name ?: ""
            phone = it.phone ?: ""
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ProfileUiState.UpdateProfileSuccess -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                profileViewModel.resetUiState()
                profileImageUri = null
            }
            is ProfileUiState.LogoutSuccess -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                profileViewModel.resetUiState()
            }
            is ProfileUiState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                user?.let {
                    name = it.name ?: ""
                    phone = it.phone ?: ""
                }
                profileImageUri = null
                profileViewModel.resetUiState()
            }
            else -> Unit
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            profileViewModel.updateProfile(name, phone, it)
        }
    }

    val showTimePicker = {
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                profileViewModel.updateReminderTime(selectedHour, selectedMinute)
                profileViewModel.toggleNotifications(true)
                AlarmScheduler.scheduleDailyAlarm(context, selectedHour, selectedMinute)
                Toast.makeText(
                    context,
                    "Daily reminders scheduled for ${reminderLabel(selectedHour, selectedMinute)}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            profileViewModel.reminderHour,
            profileViewModel.reminderMinute,
            false
        ).show()
    }

    val checkExactAlarmPermission = {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Please allow exact alarms for precise reminders", Toast.LENGTH_LONG).show()
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
            false
        } else {
            true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (checkExactAlarmPermission()) showTimePicker()
        } else {
            profileViewModel.toggleNotifications(false)
            Toast.makeText(context, "Permission denied. Reminders cannot be set.", Toast.LENGTH_LONG).show()
        }
    }

    ProfileContent(
        user = user,
        name = name,
        phone = phone,
        onNameChange = { name = it },
        onPhoneChange = { phone = it },
        pickedImage = profileImageUri,
        isSaving = uiState is ProfileUiState.UpdateProfileLoading,
        isLoggingOut = uiState is ProfileUiState.LogoutLoading,
        notificationsEnabled = profileViewModel.notificationsEnabled,
        reminderLabel = reminderLabel(profileViewModel.reminderHour, profileViewModel.reminderMinute),
        contentPadding = innerPadding,
        onPickImage = { imagePickerLauncher.launch("image/*") },
        onSave = { profileViewModel.updateProfile(name, phone, profileImageUri) },
        onRemindersToggled = { enabled ->
            if (enabled) {
                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (checkExactAlarmPermission()) {
                    showTimePicker()
                }
            } else {
                profileViewModel.toggleNotifications(false)
                AlarmScheduler.cancelDailyAlarm(context)
                Toast.makeText(context, "Reminders cancelled", Toast.LENGTH_SHORT).show()
            }
        },
        onReminderTimeClick = { if (checkExactAlarmPermission()) showTimePicker() },
        onLogout = profileViewModel::logout,
    )
}

@Composable
fun ProfileContent(
    user: UserResponse?,
    name: String,
    phone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    pickedImage: Uri?,
    isSaving: Boolean,
    isLoggingOut: Boolean,
    notificationsEnabled: Boolean,
    reminderLabel: String,
    contentPadding: PaddingValues,
    onPickImage: () -> Unit,
    onSave: () -> Unit,
    onRemindersToggled: (Boolean) -> Unit,
    onReminderTimeClick: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val profileLoaded = user != null
    val listColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("Are you sure you want to log out of Nary Gym?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag(CONFIRM_LOGOUT_TAG),
                ) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = profileLoaded, onClick = onPickImage),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        pickedImage != null -> AsyncImage(
                            model = pickedImage, contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                        !user?.profile.isNullOrBlank() -> AsyncImage(
                            model = user.profile, contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                        name.isNotBlank() -> Text(
                            name.trim().first().uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        else -> Icon(
                            Icons.Filled.Person, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp),
                        )
                    }
                }
                FilledIconButton(
                    onClick = onPickImage,
                    enabled = profileLoaded,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Change profile photo", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (profileLoaded) {
                Text(name.ifBlank { user.name.orEmpty() }, style = MaterialTheme.typography.headlineSmall)
                Text(
                    user.email.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Loading your profile…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionHeader("Profile info")
        NaryTextField(
            value = name, onValueChange = onNameChange, label = "Full name",
            leadingIcon = Icons.Filled.Person, readOnly = !profileLoaded,
        )
        NaryTextField(
            value = phone, onValueChange = onPhoneChange, label = "Phone number",
            leadingIcon = Icons.Filled.Phone, keyboardType = KeyboardType.Phone, readOnly = !profileLoaded,
        )
        NaryTextField(
            value = user?.email.orEmpty(), onValueChange = {}, label = "Email",
            leadingIcon = Icons.Filled.Email, keyboardType = KeyboardType.Email, readOnly = true,
        )

        val isDataChanged = name != (user?.name ?: "") || phone != (user?.phone ?: "")
        if (isDataChanged) {
            Button(
                onClick = onSave,
                enabled = profileLoaded && !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save changes")
                }
            }
        }

        SectionHeader("Preferences")
        ListItem(
            modifier = Modifier.clip(MaterialTheme.shapes.medium),
            headlineContent = { Text("Daily workout reminders") },
            supportingContent = { Text(if (notificationsEnabled) "Every day at $reminderLabel" else "Off") },
            leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
            trailingContent = { Switch(checked = notificationsEnabled, onCheckedChange = onRemindersToggled) },
            colors = listColors,
        )
        if (notificationsEnabled) {
            ListItem(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onReminderTimeClick),
                headlineContent = { Text("Reminder time") },
                supportingContent = { Text(reminderLabel) },
                leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                colors = listColors,
            )
        }

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            enabled = !isLoggingOut,
            modifier = Modifier.fillMaxWidth().testTag(LOGOUT_BUTTON_TAG),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
            } else {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log out")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
