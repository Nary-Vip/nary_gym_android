package com.example.narytodo.ui.screens

import android.net.Uri
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.room_db.TaskItem
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.ui.theme.grey
import com.example.narytodo.utils.copyImageToInternalStorage
import com.example.narytodo.viewModel.TaskViewModel
import com.example.narytodo.viewModel.WorkOutViewModel

@Composable
fun AllWorkOutListing(viewModel: WorkOutViewModel) {
    val workOuts by viewModel.allWorkouts.collectAsState()

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

    var workOutToEdit by remember { mutableStateOf<WorkOuts?>(null) }

    var showEditorDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    workOutToEdit = null
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),

            ) {
            Text(
                text = "Available Workouts",
                modifier = Modifier.padding(top = 32.dp),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text("${workOuts.size} remaining today", color = Color.Gray)

            Spacer(modifier = Modifier.size(20.dp))

            if (workOuts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No workOuts created.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items = workOuts, key = { it.id }) { workOut ->
                        WorkoutCard(workout = workOut, onEditClick = {
                            workOutToEdit = workOut
                            showEditorDialog = true
                        }, onDeleteClick = {
                            viewModel.delete(workOut)
                        })
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        WorkoutEditor(
            workout = workOutToEdit,
            onSave = { workout ->
                if (workOutToEdit == null) {
                    viewModel.insert(workout)
                } else {
                    viewModel.update(workout)
                }
                showEditorDialog = false
            },
            onCancel = {
                showEditorDialog = false
            }
        )
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
                        .menuAnchor()
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
