package com.nary.gym.ui.screens.CheckIn

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.checkin.hasFineLocationPermission
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.ui.theme.Accent
import com.nary.gym.ui.theme.CardBg
import com.nary.gym.ui.theme.LabelColor
import com.nary.gym.ui.theme.Primary
import com.nary.gym.ui.theme.TextColor
import com.nary.gym.utils.formatDuration
import com.nary.gym.utils.weeklySummary
import com.nary.gym.viewModel.CheckInViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CheckInScreen(
    innerPadding: PaddingValues,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val visits by viewModel.visits.collectAsState()
    val openVisit by viewModel.openVisit.collectAsState()
    val zone = viewModel.clock.zone
    var visitPendingDelete by remember { mutableStateOf<GymVisit?>(null) }

    // Ticks once a minute so "checked in for 38m" stays current.
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

    // Setting the gym only needs foreground location; the toggle handles background.
    val setGymLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (context.hasFineLocationPermission()) viewModel.useCurrentLocationAsGym()
    }

    val summary = weeklySummary(visits, Instant.ofEpochMilli(now), zone)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Gym Check-ins", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextColor)
            Text(
                "${summary.visitCount} visits · ${formatDuration(summary.totalMillis)} this week",
                fontSize = 14.sp, color = LabelColor
            )
        }

        item {
            SectionCard {
                val open = openVisit
                if (open != null) {
                    Text("Checked in", fontWeight = FontWeight.SemiBold, color = Accent)
                    Text(
                        "Since ${formatTime(open.checkInAt, zone)} · ${formatDuration(now - open.checkInAt)}",
                        color = TextColor
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::manualCheckOut) { Text("Check out") }
                } else {
                    Text("Not at the gym", fontWeight = FontWeight.SemiBold, color = TextColor)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::manualCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Check in now") }
                }
            }
        }

        item {
            SectionCard {
                Text("My gym", fontWeight = FontWeight.SemiBold, color = TextColor)
                val gym = viewModel.gym
                Text(
                    if (gym == null) "Stand inside your gym and tap below to save its location."
                    else "Saved · %.5f, %.5f".format(gym.latitude, gym.longitude),
                    color = LabelColor, fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    enabled = !viewModel.isLocating,
                    onClick = {
                        if (context.hasFineLocationPermission()) viewModel.useCurrentLocationAsGym()
                        else setGymLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                ) {
                    if (viewModel.isLocating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (gym == null) "Use current location" else "Update to current location")
                }
                if (gym != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Radius", color = LabelColor, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GymLocationStore.RADIUS_OPTIONS_METERS.forEach { radius ->
                            FilterChip(
                                selected = gym.radiusMeters == radius,
                                onClick = { viewModel.setRadius(radius) },
                                label = { Text("${radius.toInt()} m") }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AutoCheckInToggle(
                    enabled = viewModel.autoCheckInEnabled,
                    gymIsSet = gym != null,
                    onEnable = viewModel::enableAutoCheckIn,
                    onDisable = viewModel::disableAutoCheckIn
                )
            }
        }

        item { Text("History", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextColor) }

        if (visits.isEmpty()) {
            item { Text("No visits yet.", color = LabelColor) }
        }
        items(visits, key = { it.id }) { visit ->
            VisitRow(visit, zone, onDelete = { visitPendingDelete = visit })
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    visitPendingDelete?.let { visit ->
        AlertDialog(
            onDismissRequest = { visitPendingDelete = null },
            title = { Text("Delete visit?") },
            text = { Text("This visit will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVisit(visit.id)
                    visitPendingDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { visitPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun VisitRow(visit: GymVisit, zone: java.time.ZoneId, onDelete: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatDate(visit.checkInAt, zone), fontWeight = FontWeight.SemiBold, color = TextColor)
                val out = visit.checkOutAt
                Text(
                    if (out == null) "${formatTime(visit.checkInAt, zone)} – now"
                    else "${formatTime(visit.checkInAt, zone)} – ${formatTime(out, zone)} · ${formatDuration(out - visit.checkInAt)}",
                    color = TextColor, fontSize = 14.sp
                )
                val tags = buildList {
                    add(if (visit.source == VisitSource.AUTO) "Auto" else "Manual")
                    if (visit.autoClosed) add("Check-out not detected")
                }
                Text(tags.joinToString(" · "), color = LabelColor, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete visit", tint = LabelColor)
            }
        }
    }
}

private fun formatTime(millis: Long, zone: java.time.ZoneId): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(millis).atZone(zone))

private fun formatDate(millis: Long, zone: java.time.ZoneId): String =
    DateTimeFormatter.ofPattern("EEE, d MMM").format(Instant.ofEpochMilli(millis).atZone(zone))
