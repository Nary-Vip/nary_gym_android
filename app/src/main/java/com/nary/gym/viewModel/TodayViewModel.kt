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
import com.nary.gym.utils.rotationDay
import com.nary.gym.utils.getErrorMessage
import com.nary.gym.utils.weeklySummary
import com.nary.gym.widget.WorkoutWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import com.nary.gym.di.ApplicationScope
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
    val syncing: Boolean = true,
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
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val cycle = MutableStateFlow(snapshot())
    private val autoCheckInReady = MutableStateFlow(isAutoCheckInReady())
    private val syncing = MutableStateFlow(true)

    var message by mutableStateOf<String?>(null)
        private set

    private val _checkOutResults = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val checkOutResults: SharedFlow<Boolean> = _checkOutResults

    val uiState: StateFlow<TodayUiState> = combine(
        workOutRepository.getAllWorkouts(),
        gymVisitRepository.observeVisits(),
        gymVisitRepository.observeOpenVisit(),
        cycle,
        autoCheckInReady,
    ) { workouts, visits, openVisit, cycleNow, ready ->
        val day = rotationDay(cycleNow.current, cycleNow.completedToday)
        val summary = weeklySummary(visits, clock.instant(), clock.zone)
        TodayUiState(
            dayType = day.dayType,
            dayNumber = day.dayNumber,
            dayCount = day.dayCount,
            completedToday = day.completedToday,
            nextUp = day.nextUp,
            exercises = workouts.filter { it.workOutType == day.dayType },
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
                workOutRepository.sync()
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
        appScope.launch { WorkoutWidget().updateAll(appContext) }
    }

    fun checkOut() {
        viewModelScope.launch {
            val result = gymVisitRepository.checkOut()
            _checkOutResults.tryEmit(result !is CheckOutResult.NotCheckedIn)
            message = when (result) {
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
