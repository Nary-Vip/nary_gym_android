package com.nary.gym.ui.screens.Workouts

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.appwidget.updateAll
import com.nary.gym.widget.WorkoutWidget
import kotlinx.coroutines.launch
import com.nary.gym.checkin.CheckInNotificationHelper
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.common_widgets.ProfileField
import com.nary.gym.ui.screens.CheckIn.CheckInScreen
import com.nary.gym.ui.screens.Profile.ProfileScreen
import com.nary.gym.ui.screens.Workouts.AllWorkoutListing
import com.nary.gym.ui.screens.Workouts.IndividualDayWorkout
import com.nary.gym.ui.theme.grey
import com.nary.gym.utils.WorkoutCycle
import com.nary.gym.utils.copyImageToInternalStorage
import com.nary.gym.viewModel.WorkOutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun workoutEditor(
    workout: WorkOuts?,
    onSave: (WorkOuts?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var currentWorkout by remember {
        mutableStateOf(
            workout ?: WorkOuts(
                workOutName = "",
                workOutType = WorkOutType.CHEST,
                workOutImage = "",
                id = ""
            )
        )
    }

    var expanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {

            val savedPath = copyImageToInternalStorage(
                context = context,
                uri = it
            )

            currentWorkout = currentWorkout.copy(
                workOutImage = savedPath
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Color.White
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {

            Text(
                text = if (workout == null) {
                    "Create Workout"
                } else {
                    "Edit Workout"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(20.dp))
            ProfileField(
                label = "Task Name",
                value = currentWorkout.workOutName,
                onValueChange = {
                    currentWorkout = currentWorkout.copy(
                        workOutName = it
                    )
                },
                icon = Icons.Default.SportsGymnastics,
                placeholder = "Chest Press",
                keyboardType = KeyboardType.Text
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
            ) {
                ProfileField(
                    label = "Task Name",
                    value = currentWorkout.workOutType.title,
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    readOnly = true,
                    icon = Icons.Default.SportsGymnastics,
                    placeholder = "Shoulder",
                    keyboardType = KeyboardType.Text
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    WorkOutType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(type.title)
                            },
                            onClick = {
                                currentWorkout = currentWorkout.copy(
                                    workOutType = type
                                )
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
            if (currentWorkout.workOutName.isNotBlank() || currentWorkout.workOutImage.isNotBlank()) {
                val imagePath = currentWorkout.workOutImage
                AsyncImage(
                    model = imagePath,
                    contentDescription = currentWorkout.workOutName,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (currentWorkout.workOutName.isBlank()) {
                        "Select Workout Image"
                    } else {
                        "Change Workout Image"
                    }
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            Button(
                onClick = {
                    onSave(currentWorkout)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentWorkout.workOutName.isNotBlank()
            ) {
                Text("Save Workout")
            }
        }
    }
}
