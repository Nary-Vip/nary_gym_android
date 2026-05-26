package com.nary.gym.ui.screens.Home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Consumer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.nary.gym.MainActivity
import com.nary.gym.R
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.common_widgets.ProfileField
import com.nary.gym.ui.screens.Profile.ProfileScreen
import com.nary.gym.ui.screens.Workouts.AllWorkoutListing
import com.nary.gym.ui.screens.Workouts.IndividualDayWorkout
import com.nary.gym.ui.theme.grey
import com.nary.gym.utils.WorkoutCycle
import com.nary.gym.utils.WorkoutResources
import com.nary.gym.utils.copyImageToInternalStorage
import com.nary.gym.viewModel.WorkOutViewModel

@Composable
fun AllWorkOutListing (viewModel: WorkOutViewModel = hiltViewModel(), navController: NavHostController) {
    val workOuts by viewModel.allWorkouts.collectAsState()
    val context = LocalContext.current
    val activity = context as? MainActivity

    var pageIndex by remember { mutableIntStateOf(0) }

    DisposableEffect(activity) {
        val onNewIntentConsumer = Consumer<android.content.Intent> { intent ->
            if (intent.getStringExtra("source") == "notification_yes") {
                pageIndex = 1
                intent.removeExtra("source")
            }
        }
        
        // Handle initial intent
        activity?.intent?.let { intent ->
            if (intent.getStringExtra("source") == "notification_yes") {
                pageIndex = 1
                intent.removeExtra("source")
            }
        }

        activity?.addOnNewIntentListener(onNewIntentConsumer)
        onDispose {
            activity?.removeOnNewIntentListener(onNewIntentConsumer)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(true) {
        viewModel.getCurrentUser()
    }

    var currentWorkout by remember {
        mutableStateOf<WorkOuts?>(null)
    }

    var showEditorDialog by remember { mutableStateOf(false) }

    val workoutCycle = WorkoutCycle(context)
    var workoutType by remember { mutableStateOf(workoutCycle.getCurrentDayWorkout()) }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(100.dp),
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        pageIndex = 0
                    }) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Today", modifier = Modifier.size(24.dp), tint = Color.White)
                    }

                    IconButton(onClick = {
                        pageIndex = 1
                    }) {
                        Icon(imageVector = Icons.Default.AllInbox, contentDescription = "Today", modifier = Modifier.size(24.dp), tint = Color.White)
                    }
                    IconButton(onClick = {
                        pageIndex = 2
                    }) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Today", modifier = Modifier.size(24.dp), tint = Color.White)
                    }
                }
            }
        },
        floatingActionButton = {
            if (pageIndex == 0) {
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
        when (pageIndex) {
            0 -> {
                AllWorkoutListing(innerPadding, workOuts, viewModel)
            }
            1 -> {
                IndividualDayWorkout(
                    workOutType = workoutType,
                    viewModel = viewModel,
                    innerPadding = innerPadding,
                    onComplete = {
                        workoutType = workoutCycle.moveToNextDayWorkOut()
                    }
                )
            }
            else -> {
                ProfileScreen(innerPadding)
            }
        }
        if (showEditorDialog) {
            WorkoutEditor(
                workout = null,
                onSave = { workout ->
                    if (workout != null) {
                        viewModel.insert(workout)
                    }
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
    onSave: (WorkOuts?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var currentWorkout by remember {
        mutableStateOf(
            workout ?: WorkOuts(workOutName = "", workOutType = WorkOutType.CHEST, workOutImage = "", id = "")
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
                value = currentWorkout.workOutName ?: "",
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
                    value = currentWorkout?.workOutType?.title ?: "",
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
            if (!currentWorkout.workOutName.isBlank() || !currentWorkout.workOutImage.isBlank()) {
                val imagePath = currentWorkout.workOutImage
                if (imagePath.contains("/") || imagePath.startsWith("http")) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = currentWorkout.workOutName,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val resId = WorkoutResources.drawableMap[imagePath]
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
                enabled = !currentWorkout.workOutName.isNullOrBlank()
            ) {
                Text("Save Workout")
            }
        }
    }
}
