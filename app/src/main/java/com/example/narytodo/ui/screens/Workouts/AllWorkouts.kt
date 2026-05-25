package com.example.narytodo.ui.screens.Workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.ui.screens.Home.WorkoutEditor
import com.example.narytodo.ui.screens.WorkoutCard
import com.example.narytodo.viewModel.WorkOutViewModel

@Composable
fun AllWorkoutListing(
    innerPadding: PaddingValues,
    workOuts: List<WorkOuts>,
    viewModel: WorkOutViewModel
) {
    val groupedWorkouts = workOuts.groupBy { it.workOutType }
    var workOutToEdit by remember { mutableStateOf<WorkOuts?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.fetchWorkouts()
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = 24.dp)
            .fillMaxSize(),

        ) {
        Text(
            text = "All Workouts",
            modifier = Modifier.padding(top = 32.dp),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text("${workOuts.size} workouts added", color = Color.Gray)

        Spacer(modifier = Modifier.size(20.dp))

        if (viewModel.isLoading && workOuts.isEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(5) {
                    WorkoutCard(isLoading = true)
                }
            }
        } else if (workOuts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workOuts created.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedWorkouts.forEach { (type, workouts) ->
                    item {
                        Text(
                            text = "${type.title} Workouts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    items(
                        items = workouts,
                        key = { it.id }
                    ) { workOut ->

                        WorkoutCard(
                            workout = workOut,
                            onEditClick = {
                                workOutToEdit = workOut
                                showEditorDialog = true
                            },
                            onDeleteClick = {
                                viewModel.delete(workOut)
                            }
                        )
                    }
                }
            }
        }
    }
    if (showEditorDialog) {
        WorkoutEditor(
            workout = workOutToEdit,
            onSave = { workout ->
                if (workout != null) {
                    if (workOutToEdit == null) {
                        viewModel.insert(workout)
                    } else {
                        viewModel.update(workout)
                    }
                }
                showEditorDialog = false
            },
            onCancel = {
                showEditorDialog = false
            }
        )
    }

}

