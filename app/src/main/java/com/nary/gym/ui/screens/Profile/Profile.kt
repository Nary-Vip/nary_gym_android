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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nary.gym.Notifications.AlarmScheduler
import com.nary.gym.Notifications.NotificationHelper
import com.nary.gym.ui.common_widgets.ProfileField
import com.nary.gym.ui.theme.Accent
import com.nary.gym.ui.theme.CardBg
import com.nary.gym.ui.theme.DividerColor
import com.nary.gym.ui.theme.LabelColor
import com.nary.gym.ui.theme.Primary
import com.nary.gym.ui.theme.TextColor
import com.nary.gym.viewModel.ProfileUiState
import com.nary.gym.viewModel.ProfileViewModel

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
    var email by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Initialize fields from user data when it arrives
    LaunchedEffect(user) {
        user?.let {
            name = it.name ?: ""
            phone = it.phone ?: ""
            email = it.email ?: ""
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
                    name = it.name
                    phone = it.phone
                    email = it.email
                }
                profileImageUri = null
                profileViewModel.resetUiState()
            }

            else -> Unit
        }
    }

    val genderOptions = listOf("Male", "Female")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            profileViewModel.updateProfile(name, phone, it)
        }
    }

    val showTimePicker = {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                profileViewModel.updateReminderTime(selectedHour, selectedMinute)
                profileViewModel.toggleNotifications(true)
                AlarmScheduler.scheduleDailyAlarm(context, selectedHour, selectedMinute)
                Toast.makeText(
                    context,
                    "Daily reminders scheduled for ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            profileViewModel.reminderHour,
            profileViewModel.reminderMinute,
            false
        )
        timePickerDialog.show()
    }

    val checkExactAlarmPermission = {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Please allow exact alarms for precise reminders", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            false
        } else {
            true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (checkExactAlarmPermission()) {
                showTimePicker()
            }
        } else {
            profileViewModel.toggleNotifications(false)
            Toast.makeText(context, "Permission denied. Reminders cannot be set.", Toast.LENGTH_LONG).show()
        }
    }

if (showLogoutDialog) {
    AlertDialog(
        onDismissRequest = { showLogoutDialog = false },
        containerColor = Color.White,
        title = {
            Text(
                "Log out?",
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
        },
        text = {
            Text(
                "Are you sure you want to log out of Nary Gym?",
                color = LabelColor
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showLogoutDialog = false
                    profileViewModel.logout()
                }
            ) {
                Text("Log out", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { showLogoutDialog = false }) {
                Text("Cancel", color = LabelColor)
            }
        }
    )
}

Column(
modifier = Modifier
.fillMaxSize()
.padding(innerPadding)
.background(CardBg)
.verticalScroll(rememberScrollState())
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Primary, Color(0xFF16213E))
                )
            )
            .padding(top = 56.dp, bottom = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NARY GYM",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = Accent
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, Accent, CircleShape)
                        .background(Color(0xFF2A2A4A))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!user?.profile.isNullOrBlank()) {
                        AsyncImage(
                            model = user?.profile,
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile placeholder",
                            tint = Color(0xFF5A5A8A),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Accent)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pick image",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name.orEmpty().ifBlank { "Your Name" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email,
                    fontSize = 13.sp,
                    color = Color(0xFF9090BB)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Profile Info",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LabelColor,
            letterSpacing = 1.sp
        )
        // The profile is fetched on every app start and the backend can take a while to wake up;
        // until it arrives the fields are blank, so say so and don't allow editing/saving.
        val profileLoaded = user != null
        if (!profileLoaded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = LabelColor)
                Text("  Loading your profile…", fontSize = 13.sp, color = LabelColor)
            }
        }
        ProfileField(
            label = "Full Name",
            value = name,
            readOnly = !profileLoaded,
            onValueChange = { name = it },
            icon = Icons.Default.Person,
            placeholder = "Enter your name",
            keyboardType = KeyboardType.Text
        )
        // Phone field
        ProfileField(
            label = "Phone Number",
            value = phone,
            readOnly = !profileLoaded,
            onValueChange = { phone = it },
            icon = Icons.Default.Phone,
            placeholder = "+91 00000 00000",
            keyboardType = KeyboardType.Phone
        )
        ProfileField(
            label = "Email",
            value = email,
            readOnly = true,
            onValueChange = { email = it },
            icon = Icons.Default.Email,
            placeholder = "you@example.com",
            keyboardType = KeyboardType.Email
        )

        val isDataChanged = name != (user?.name ?: "") || phone != (user?.phone ?: "")

        if (isDataChanged) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    profileViewModel.updateProfile(name, phone, profileImageUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = profileLoaded && uiState !is ProfileUiState.UpdateProfileLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (uiState is ProfileUiState.UpdateProfileLoading)
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                else
                    Text(
                        text = "Save Changes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = DividerColor
        )
        Text(
            text = "Preferences",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LabelColor,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily Workout Reminders",
                    fontSize = 15.sp,
                    color = TextColor,
                    fontWeight = FontWeight.Medium
                )
                if (profileViewModel.notificationsEnabled) {
                    val formattedTime = remember(profileViewModel.reminderHour, profileViewModel.reminderMinute) {
                        val hour = profileViewModel.reminderHour
                        val minute = profileViewModel.reminderMinute
                        val amPm = if (hour < 12) "AM" else "PM"
                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                        String.format("%02d:%02d %s", displayHour, minute, amPm)
                    }
                    Text(
                        text = "Scheduled for $formattedTime",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Switch(
                checked = profileViewModel.notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                if (checkExactAlarmPermission()) {
                                    showTimePicker()
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            if (checkExactAlarmPermission()) {
                                showTimePicker()
                            }
                        }
                    } else {
                        profileViewModel.toggleNotifications(false)
                        AlarmScheduler.cancelDailyAlarm(context)
                        Toast.makeText(context, "Reminders cancelled", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = DividerColor
        )
        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = uiState !is ProfileUiState.LogoutLoading,
            shape = RoundedCornerShape(14.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(Accent, Accent))
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Accent
            )
        ) {
            if (uiState is ProfileUiState.LogoutLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Accent,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
}
