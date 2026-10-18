package com.nary.gym.ui.screens.Today

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.components.EmptyState
import com.nary.gym.ui.components.LiveStatusCard
import com.nary.gym.ui.components.LoadingRow
import com.nary.gym.ui.components.SectionHeader
import com.nary.gym.ui.components.StatTile
import com.nary.gym.ui.components.WorkoutListItem
import com.nary.gym.ui.screens.Workouts.WorkoutEditor
import com.nary.gym.utils.formatClockTime
import com.nary.gym.utils.formatDuration
import com.nary.gym.viewModel.TodayUiState
import com.nary.gym.viewModel.TodayViewModel
import com.nary.gym.viewModel.WorkOutViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val TODAY_LIST_TAG = "today_list"

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onOpenCheckIns: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
    workOutViewModel: WorkOutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<WorkOuts?>(null) }
    var adding by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(workOutViewModel.errorMessage) {
        workOutViewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            workOutViewModel.clearError()
        }
    }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000)
            value = System.currentTimeMillis()
        }
    }

    RefreshOnNewDay(now, ZoneId.systemDefault(), viewModel::refresh)

    TodayContent(
        state = state,
        now = now,
        contentPadding = contentPadding,
        onMarkComplete = viewModel::markComplete,
        onCheckOut = viewModel::checkOut,
        onSetUpCheckIn = onOpenCheckIns,
        onExerciseClick = { editing = it },
        onAddWorkout = { adding = true },
    )

    editing?.let { workout ->
        WorkoutEditor(
            workout = workout,
            onSave = { workOutViewModel.update(it); editing = null },
            onCancel = { editing = null },
            onDelete = { workOutViewModel.delete(it); editing = null },
        )
    }
    if (adding) {
        WorkoutEditor(
            workout = null,
            initialType = state.dayType,
            onSave = { workOutViewModel.insert(it); adding = false },
            onCancel = { adding = false },
        )
    }
}

@Composable
fun TodayContent(
    state: TodayUiState,
    now: Long,
    contentPadding: PaddingValues,
    onMarkComplete: () -> Unit,
    onCheckOut: () -> Unit,
    onSetUpCheckIn: () -> Unit,
    onExerciseClick: (WorkOuts) -> Unit,
    onAddWorkout: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val haptics = LocalHapticFeedback.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TODAY_LIST_TAG),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                DateTimeFormatter.ofPattern("EEEE, d MMM").format(Instant.ofEpochMilli(now).atZone(zone)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { DayHero(state, onMarkComplete) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${state.weekVisits}", "visits this week", Modifier.weight(1f))
                StatTile(formatDuration(state.weekMillis), "at the gym", Modifier.weight(1f))
            }
        }
        val open = state.openVisit
        if (open != null) {
            item {
                LiveStatusCard(
                    title = "Checked in",
                    detail = "Since ${formatClockTime(open.checkInAt, zone)} · ${formatDuration(now - open.checkInAt)}",
                    actionLabel = "Check out",
                    onAction = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onCheckOut()
                    },
                )
            }
        } else if (!state.autoCheckInReady) {
            item { TextButton(onClick = onSetUpCheckIn) { Text("Set up auto check-in") } }
        }
        item { SectionHeader("Today's exercises", count = state.exercises.size) }
        if (state.exercises.isEmpty() && state.syncing) {
            item { LoadingRow("Loading workouts…") }
        } else if (state.exercises.isEmpty()) {
            item {
                EmptyState(
                    title = "No ${state.dayType.title} exercises yet",
                    body = "Add the exercises you do on ${state.dayType.title} day.",
                    actionLabel = "Add a ${state.dayType.title} workout",
                    onAction = onAddWorkout,
                )
            }
        }
        items(state.exercises, key = { it.id }) { workout ->
            WorkoutListItem(workout, onClick = { onExerciseClick(workout) }, modifier = Modifier.clip(MaterialTheme.shapes.medium))
        }
    }
}

@Composable
private fun DayHero(state: TodayUiState, onMarkComplete: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("DAY ${state.dayNumber} OF ${state.dayCount}", style = MaterialTheme.typography.labelMedium)
            Text("${state.dayType.title} Day", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            RotationProgress(state.dayNumber, state.dayCount, state.completedToday)
            Spacer(Modifier.height(16.dp))
            if (state.completedToday) {
                Text("Done for today 💪", style = MaterialTheme.typography.titleLarge)
                Text("Next up: ${state.nextUp.title}", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("Work hard babe", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onMarkComplete()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as complete")
                }
            }
        }
    }
}

/** One segment per rotation day; segments before today are filled, today's fills once done. */
@Composable
private fun RotationProgress(dayNumber: Int, dayCount: Int, completed: Boolean) {
    val on = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "Day $dayNumber of $dayCount" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(dayCount) { index ->
            val alpha = when {
                index < dayNumber - 1 -> 1f
                index == dayNumber - 1 -> if (completed) 1f else 0.5f
                else -> 0.2f
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(on.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
fun RefreshOnNewDay(now: Long, zone: ZoneId, onNewDay: () -> Unit) {
    val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val currentOnNewDay by rememberUpdatedState(onNewDay)
    var lastDay by remember { mutableStateOf(day) }
    LaunchedEffect(day) {
        if (day != lastDay) {
            lastDay = day
            currentOnNewDay()
        }
    }
}
