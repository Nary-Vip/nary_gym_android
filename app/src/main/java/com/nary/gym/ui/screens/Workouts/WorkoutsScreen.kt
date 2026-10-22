package com.nary.gym.ui.screens.Workouts

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.components.EmptyState
import com.nary.gym.ui.components.LoadingRow
import com.nary.gym.ui.components.SectionHeader
import com.nary.gym.ui.components.WorkoutListItem
import com.nary.gym.viewModel.WorkOutViewModel

@Composable
fun WorkoutsScreen(contentPadding: PaddingValues, viewModel: WorkOutViewModel = hiltViewModel()) {
    val workouts by viewModel.allWorkouts.collectAsState()
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WorkOuts?>(null) }

    LaunchedEffect(Unit) { viewModel.fetchWorkouts() }
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    WorkoutsContent(
        workouts = workouts,
        contentPadding = contentPadding,
        onAdd = { creating = true },
        onEdit = { editing = it },
        onDelete = { workout, onFailed -> viewModel.delete(workout, onFailure = onFailed) },
        isLoading = viewModel.isLoading,
    )

    if (creating) {
        WorkoutEditor(
            workout = null,
            onSave = { viewModel.insert(it); creating = false },
            onCancel = { creating = false },
            existingNames = workouts.namesExcept(null),
        )
    }
    editing?.let { workout ->
        WorkoutEditor(
            workout = workout,
            onSave = { viewModel.update(it); editing = null },
            onCancel = { editing = null },
            onDelete = { viewModel.delete(it); editing = null },
            existingNames = workouts.namesExcept(workout),
        )
    }
}

fun List<WorkOuts>.namesExcept(workout: WorkOuts?): Set<String> =
    filter { it.id != workout?.id }.map { it.workOutName.trim() }.toSet()

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsContent(
    workouts: List<WorkOuts>,
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onEdit: (WorkOuts) -> Unit,
    onDelete: (WorkOuts, onFailed: () -> Unit) -> Unit,
    isLoading: Boolean = false,
) {
    var filter by rememberSaveable { mutableStateOf<WorkOutType?>(null) } 
    var pendingDelete by remember { mutableStateOf<WorkOuts?>(null) }
    val committedIds = remember { mutableStateListOf<String>() }
    val snackbar = remember { SnackbarHostState() }
    val currentOnDelete by rememberUpdatedState(onDelete)

    fun commit(workout: WorkOuts) {
        committedIds += workout.id
        currentOnDelete(workout) { committedIds -= workout.id }
    }

    DisposableEffect(Unit) { onDispose { pendingDelete?.let { currentOnDelete(it) {} } } }

    LaunchedEffect(pendingDelete) {
        val workout = pendingDelete ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "Deleted ${workout.workOutName}",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.Dismissed && workout.id !in committedIds) commit(workout)
        if (pendingDelete?.id == workout.id) pendingDelete = null
    }

    val visible = workouts.filter { it.id != pendingDelete?.id && it.id !in committedIds }
    val shown = visible.filter { filter == null || it.workOutType == filter }

    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "${visible.size} exercises",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") }, colors = chipColors, modifier = Modifier.testTag("filter_ALL"))
                    WorkOutType.entries.forEach { type ->
                        FilterChip(
                            selected = filter == type,
                            onClick = { filter = type },
                            label = { Text(type.title) },
                            colors = chipColors,
                            modifier = Modifier.testTag("filter_${type.name}"),
                        )
                    }
                }
            }
            if (isLoading && workouts.isEmpty()) {
                item { LoadingRow("Loading workouts…") }
            } else if (shown.isEmpty()) {
                item {
                    EmptyState(
                        title = if (filter == null) "No workouts yet" else "No ${filter!!.title} workouts",
                        body = "Tap Add workout to create one.",
                    )
                }
            }
            WorkOutType.entries.forEach { type ->
                val group = shown.filter { it.workOutType == type }
                if (group.isNotEmpty()) {
                    stickyHeader(key = "header-${type.name}") {
                        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(type.title.uppercase(), count = group.size, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    items(group, key = { it.id }) { workout ->
                        SwipeToDeleteRow(
                            workout = workout,
                            onSwiped = {
                                pendingDelete?.let { commit(it) } // a second swipe commits the first
                                pendingDelete = workout
                            },
                            onClick = { onEdit(workout) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) } // room for the FAB
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add workout") },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(workout: WorkOuts, onSwiped: () -> Unit, onClick: () -> Unit) {
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.targetValue) {
        if (state.targetValue == SwipeToDismissBoxValue.EndToStart) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onSwiped() },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        WorkoutListItem(
            workout,
            onClick = onClick,
            modifier = Modifier.semantics {
                customActions = listOf(CustomAccessibilityAction("Delete") { onSwiped(); true })
            },
        )
    }
}
