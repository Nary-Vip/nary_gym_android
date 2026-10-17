package com.nary.gym.ui.screens.CheckIn

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.checkin.hasFineLocationPermission
import com.nary.gym.data.local.GymLocation
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.ui.components.EmptyState
import com.nary.gym.ui.components.LiveStatusCard
import com.nary.gym.ui.components.SectionHeader
import com.nary.gym.ui.components.StatTile
import com.nary.gym.utils.formatClockTime
import com.nary.gym.utils.formatDayLabel
import com.nary.gym.utils.formatDuration
import com.nary.gym.utils.weeklySummary
import com.nary.gym.viewModel.CheckInViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

const val CHECK_IN_LIST_TAG = "check_in_list"

@Composable
fun CheckInScreen(
    innerPadding: PaddingValues,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val visits by viewModel.visits.collectAsState()
    val openVisit by viewModel.openVisit.collectAsState()

    val now by produceState(viewModel.clock.millis()) {
        while (true) {
            delay(60_000)
            value = viewModel.clock.millis()
        }
    }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val setGymLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (context.hasFineLocationPermission()) viewModel.useCurrentLocationAsGym()
    }

    CheckInContent(
        visits = visits,
        openVisit = openVisit,
        gym = viewModel.gym,
        autoCheckInEnabled = viewModel.autoCheckInEnabled,
        isLocating = viewModel.isLocating,
        now = now,
        zone = viewModel.clock.zone,
        contentPadding = innerPadding,
        onCheckIn = viewModel::manualCheckIn,
        onCheckOut = viewModel::manualCheckOut,
        onUseCurrentLocation = {
            if (context.hasFineLocationPermission()) viewModel.useCurrentLocationAsGym()
            else setGymLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        },
        onRadiusSelected = viewModel::setRadius,
        onEnableAutoCheckIn = viewModel::enableAutoCheckIn,
        onDisableAutoCheckIn = viewModel::disableAutoCheckIn,
        onDeleteVisit = viewModel::deleteVisit,
    )
}

@Composable
fun CheckInContent(
    visits: List<GymVisit>,
    openVisit: GymVisit?,
    gym: GymLocation?,
    autoCheckInEnabled: Boolean,
    isLocating: Boolean,
    now: Long,
    zone: ZoneId,
    contentPadding: PaddingValues,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onRadiusSelected: (Float) -> Unit,
    onEnableAutoCheckIn: () -> Unit,
    onDisableAutoCheckIn: () -> Unit,
    onDeleteVisit: (String) -> Unit,
) {
    var visitPendingDelete by remember { mutableStateOf<GymVisit?>(null) }
    val summary = weeklySummary(visits, Instant.ofEpochMilli(now), zone)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(CHECK_IN_LIST_TAG),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${summary.visitCount}", "visits this week", Modifier.weight(1f))
                StatTile(formatDuration(summary.totalMillis), "at the gym", Modifier.weight(1f))
            }
        }

        item {
            if (openVisit != null) {
                LiveStatusCard(
                    title = "Checked in",
                    detail = "Since ${formatClockTime(openVisit.checkInAt, zone)} · ${formatDuration(now - openVisit.checkInAt)}",
                    actionLabel = "Check out",
                    onAction = onCheckOut,
                )
            } else {
                Card(shape = MaterialTheme.shapes.medium, colors = cardColors) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Not at the gym", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Check in yourself, or let auto check-in do it.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = onCheckIn) { Text("Check in now") }
                    }
                }
            }
        }

        item { SectionHeader("My gym") }
        item {
            Card(shape = MaterialTheme.shapes.medium, colors = cardColors) {
                Column(Modifier.padding(bottom = 16.dp)) {
                    ListItem(
                        headlineContent = { Text(if (gym == null) "No gym saved" else "Gym location saved") },
                        supportingContent = {
                            Text(
                                if (gym == null) "Stand inside your gym and use your current location."
                                else String.format(Locale.US, "%.5f, %.5f", gym.latitude, gym.longitude)
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    )
                    OutlinedButton(
                        enabled = !isLocating,
                        onClick = onUseCurrentLocation,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        if (isLocating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(if (gym == null) "Use current location" else "Update to current location")
                    }
                    if (gym != null) {
                        Text(
                            "Radius",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                        )
                        Row(
                            Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GymLocationStore.RADIUS_OPTIONS_METERS.forEach { radius ->
                                FilterChip(
                                    selected = gym.radiusMeters == radius,
                                    onClick = { onRadiusSelected(radius) },
                                    label = { Text("${radius.toInt()} m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                        AutoCheckInToggle(
                            enabled = autoCheckInEnabled,
                            gymIsSet = gym != null,
                            onEnable = onEnableAutoCheckIn,
                            onDisable = onDisableAutoCheckIn
                        )
                    }
                }
            }
        }

        item { SectionHeader("History", count = visits.size.takeIf { it > 0 }) }
        if (visits.isEmpty()) {
            item { EmptyState("No visits yet", "Your gym visits will appear here.") }
        }
        visits.groupBy { formatDayLabel(it.checkInAt, zone) }.forEach { (day, dayVisits) ->
            item(key = "day-$day") {
                Text(day, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            }
            items(dayVisits, key = { it.id }) { visit ->
                VisitRow(visit, zone, onDelete = { visitPendingDelete = visit })
            }
        }
    }

    visitPendingDelete?.let { visit ->
        AlertDialog(
            onDismissRequest = { visitPendingDelete = null },
            title = { Text("Delete visit?") },
            text = { Text("This visit will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVisit(visit.id)
                        visitPendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { visitPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VisitRow(visit: GymVisit, zone: ZoneId, onDelete: () -> Unit) {
    val out = visit.checkOutAt
    val tags = buildList {
        add(if (visit.source == VisitSource.AUTO) "Auto" else "Manual")
        if (visit.autoClosed) add("Check-out not detected")
    }
    ListItem(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
        headlineContent = {
            Text(
                if (out == null) "${formatClockTime(visit.checkInAt, zone)} – now"
                else "${formatClockTime(visit.checkInAt, zone)} – ${formatClockTime(out, zone)} · ${formatDuration(out - visit.checkInAt)}"
            )
        },
        supportingContent = { Text(tags.joinToString(" · ")) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete visit on ${formatDayLabel(visit.checkInAt, zone)}",
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}
