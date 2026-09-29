package com.nary.gym.ui.screens.Home

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.ui.screens.Workouts.AllWorkoutListing
import com.nary.gym.ui.screens.Workouts.IndividualDayWorkout
import com.nary.gym.ui.screens.Workouts.workoutEditor
import com.nary.gym.utils.WorkoutCycle
import com.nary.gym.viewModel.WorkOutViewModel
import com.nary.gym.widget.WorkoutWidget
import kotlinx.coroutines.launch

/*
 * Part 1 of the M3 redesign hosts the existing screens unchanged inside the new tabs.
 * These wrappers hold the state HomeScreen used to own; Part 2 replaces them with the
 * redesigned Today and Workouts screens.
 */

@Composable
private fun ErrorToasts(viewModel: WorkOutViewModel) {
    val context = LocalContext.current
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
}

@Composable
fun LegacyTodayTab(contentPadding: PaddingValues, viewModel: WorkOutViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workoutCycle = remember { WorkoutCycle(context) }
    var workoutType by remember { mutableStateOf(workoutCycle.getCurrentDayWorkout()) }
    var isCompletedToday by remember { mutableStateOf(workoutCycle.hasCompletedToday()) }
    ErrorToasts(viewModel)

    IndividualDayWorkout(
        workOutType = workoutType,
        viewModel = viewModel,
        innerPadding = contentPadding,
        isCompletedToday = isCompletedToday,
        onComplete = {
            workoutType = workoutCycle.moveToNextDayWorkOut()
            isCompletedToday = workoutCycle.hasCompletedToday()
            scope.launch { WorkoutWidget().updateAll(context) }
        },
    )
}

@Composable
fun LegacyWorkoutsTab(contentPadding: PaddingValues, viewModel: WorkOutViewModel = hiltViewModel()) {
    val workOuts by viewModel.allWorkouts.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    ErrorToasts(viewModel)

    Box(Modifier.fillMaxSize()) {
        AllWorkoutListing(contentPadding, workOuts, viewModel)
        ExtendedFloatingActionButton(
            onClick = { showEditor = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add workout") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(contentPadding)
                .padding(16.dp),
        )
    }
    if (showEditor) {
        workoutEditor(
            workout = null,
            onSave = { workout ->
                if (workout != null) viewModel.insert(workout)
                showEditor = false
            },
            onCancel = { showEditor = false },
        )
    }
}
