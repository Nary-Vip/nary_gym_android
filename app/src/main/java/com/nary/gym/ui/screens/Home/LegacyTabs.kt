package com.nary.gym.ui.screens.Home

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.ui.screens.Workouts.IndividualDayWorkout
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
