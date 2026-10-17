package com.nary.gym.viewModel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.repository.local.CheckOutResult
import com.nary.gym.repository.local.GymVisitRepository
import com.nary.gym.repository.remote.WorkOutRepository
import com.nary.gym.utils.WorkoutCycle
import com.nary.gym.utils.formatDuration
import com.nary.gym.utils.getErrorMessage
import com.nary.gym.utils.weeklySummary
import com.nary.gym.widget.WorkoutWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val dayType: WorkOutType = WorkOutType.LEG,
    val dayNumber: Int = 1,
    val dayCount: Int = WorkOutType.entries.size,
    val completedToday: Boolean = false,
    val nextUp: WorkOutType = WorkOutType.CHEST,
    val exercises: List<WorkOuts> = emptyList(),
    val weekVisits: Int = 0,
    val weekMillis: Long = 0,
    val openVisit: GymVisit? = null,
    val autoCheckInReady: Boolean = true,
    val syncing: Boolean = false,
)

private data class CycleSnapshot(val current: WorkOutType, val completedToday: Boolean, val date: LocalDate)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workOutRepository: WorkOutRepository,
    private val gymVisitRepository: GymVisitRepository,
    private val gymLocationStore: GymLocationStore,
    private val workoutCycle: WorkoutCycle,
    private val clock: Clock,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val cycle = MutableStateFlow(snapshot())
    private val autoCheckInReady = MutableStateFlow(isAutoCheckInReady())
    private val syncing = MutableStateFlow(true)

    var message by mutableStateOf<String?>(null)
        private set

    val uiState: StateFlow<TodayUiState> = combine(
        workOutRepository.getAllWorkouts(),
        gymVisitRepository.observeVisits(),
        gymVisitRepository.observeOpenVisit(),
        cycle,
        autoCheckInReady,
    ) { workouts, visits, openVisit, cycleNow, ready ->
        val types = WorkOutType.entries
        val currentIndex = types.indexOf(cycleNow.current)
        val dayIndex = if (cycleNow.completedToday) (currentIndex - 1 + types.size) % types.size else currentIndex
        val dayType = types[dayIndex]
        val summary = weeklySummary(visits, clock.instant(), clock.zone)
        TodayUiState(
            dayType = dayType,
            dayNumber = dayIndex + 1,
            dayCount = types.size,
            completedToday = cycleNow.completedToday,
            nextUp = types[(dayIndex + 1) % types.size],
            exercises = workouts.filter { it.workOutType == dayType },
            weekVisits = summary.visitCount,
            weekMillis = summary.totalMillis,
            openVisit = openVisit,
            autoCheckInReady = ready,
        )
    }.combine(syncing) { state, isSyncing -> state.copy(syncing = isSyncing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    init {
        viewModelScope.launch {
            try {
                workOutRepository.syncWorkouts()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    ensureActive()
                }
                message = e.getErrorMessage()
            } finally {
                syncing.value = false
            }
        }
    }

    fun markComplete() {
        workoutCycle.moveToNextDayWorkOut()
        cycle.value = snapshot()
        viewModelScope.launch { WorkoutWidget().updateAll(appContext) }
    }

    fun checkOut() {
        viewModelScope.launch {
            message = when (val result = gymVisitRepository.checkOut()) {
                is CheckOutResult.CheckedOut ->
                    "Checked out · ${formatDuration(result.visit.checkOutAt!! - result.visit.checkInAt)}"
                CheckOutResult.Discarded -> "Visit under 5 minutes discarded"
                CheckOutResult.NotCheckedIn -> "You're not checked in"
            }
        }
    }

    fun refresh() {
        cycle.value = snapshot()
        autoCheckInReady.value = isAutoCheckInReady()
    }

    fun clearMessage() {
        message = null
    }

    private fun snapshot() = CycleSnapshot(workoutCycle.getCurrentDayWorkout(), workoutCycle.hasCompletedToday(), LocalDate.now(clock))

    private fun isAutoCheckInReady() =
        gymLocationStore.isAutoCheckInEnabled() && gymLocationStore.getGym() != null
}
