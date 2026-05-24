package com.example.narytodo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.narytodo.R
import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.navigation.NavRoutes
import com.example.narytodo.ui.theme.grey
import com.example.narytodo.utils.WorkoutCycle
import com.example.narytodo.utils.WorkoutResources
import com.example.narytodo.utils.copyImageToInternalStorage
import com.example.narytodo.viewModel.WorkOutViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

@Composable
fun AllWorkOutListing (viewModel: WorkOutViewModel = hiltViewModel(), navController: NavHostController) {
    val workOuts by viewModel.allWorkouts.collectAsState()
    val context = LocalContext.current

    var currentWorkout by remember {
        mutableStateOf(
            WorkOuts(
                workOutName = "",
                workOutType = WorkOutType.CHEST,
                workOutImage = "",
                dayId = 0
            )
        )
    }

    var pageIndex by remember { mutableIntStateOf(0) }
    var showEditorDialog by remember { mutableStateOf(false) }

    val workoutCycle = WorkoutCycle(context)
    var workoutType by remember { mutableStateOf(workoutCycle.getCurrentDayWorkout()) }

    Scaffold(
        bottomBar = {
            BottomAppBar() {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        pageIndex = 0
                    }) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Today")
                    }

                    IconButton(onClick = {
                        pageIndex = 1
                    }) {
                        Icon(imageVector = Icons.Default.AllInbox, contentDescription = "Today")
                    }
                    IconButton(onClick = {
                        pageIndex = 2
                    }) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Today")
                    }

                }
            }
        },
        floatingActionButton = {
            if (pageIndex == 1) {
                ExtendedFloatingActionButton(
                    onClick = {
                        showEditorDialog = true
                    }, shape = RoundedCornerShape(20.dp),
                    containerColor = grey,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Button")
                    Text("New workOuts", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    ) { innerPadding ->
        if (pageIndex == 0) {
            IndividualDayWorkout(
                workOutType = workoutType,
                viewModel = viewModel,
                innerPadding = innerPadding,

                onComplete = {
                    workoutType = workoutCycle.moveToNextDayWorkOut()
                }
            )
        } else if (pageIndex == 1){
            AllWorkoutListing(innerPadding, workOuts, viewModel)
        } else {
            ProfileScreen(innerPadding, onLogout = {
                navController.navigate(NavRoutes.Login.route)
            })
        }
        if (showEditorDialog) {
            WorkoutEditor(
                workout = null,
                onSave = { workout ->
                    viewModel.insert(workout)
                    showEditorDialog = false
                },
                onCancel = {
                    showEditorDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditor(
    workout: WorkOuts?,
    onSave: (WorkOuts) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var currentWorkout by remember {
        mutableStateOf(
            workout ?: WorkOuts(
                workOutName = "",
                workOutType = WorkOutType.CHEST,
                workOutImage = "",
                dayId = 0
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
            OutlinedTextField(
                value = currentWorkout.workOutName,
                onValueChange = {
                    currentWorkout = currentWorkout.copy(
                        workOutName = it
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Workout Name")
                }
            )

            Spacer(modifier = Modifier.size(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
            ) {
                OutlinedTextField(
                    value = currentWorkout.workOutType.title,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    label = {
                        Text("Workout Type")
                    }
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
            if (!currentWorkout.workOutImage.isBlank()) {
                if (currentWorkout.workOutImage.contains("/")) {
                    AsyncImage(
                        model = currentWorkout.workOutImage,
                        contentDescription = currentWorkout.workOutName,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val resId = WorkoutResources.drawableMap[currentWorkout.workOutImage]
                        ?: R.drawable.placeholder
                    Image(
                        painter = painterResource(resId),
                        contentDescription = "Workout Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (currentWorkout.workOutImage.isBlank()) {
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
