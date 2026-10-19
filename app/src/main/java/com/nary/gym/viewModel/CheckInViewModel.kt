package com.nary.gym.viewModel

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nary.gym.checkin.CurrentLocationProvider
import com.nary.gym.checkin.GymGeofenceManager
import com.nary.gym.data.local.GymLocation
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.repository.local.CheckInResult
import com.nary.gym.repository.local.CheckOutResult
import com.nary.gym.repository.local.GymVisitRepository
import com.nary.gym.utils.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val repository: GymVisitRepository,
    private val store: GymLocationStore,
    private val geofenceManager: GymGeofenceManager,
    private val locationProvider: CurrentLocationProvider,
    val clock: Clock,
) : ViewModel() {

    val visits: StateFlow<List<GymVisit>> = repository.observeVisits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val openVisit: StateFlow<GymVisit?> = repository.observeOpenVisit()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = null)

    var gym by mutableStateOf(store.getGym())
        private set

    var autoCheckInEnabled by mutableStateOf(store.isAutoCheckInEnabled())
        private set

    var isLocating by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    private val _actionResults = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val actionResults: SharedFlow<Boolean> = _actionResults


    fun useCurrentLocationAsGym() {
        viewModelScope.launch {
            isLocating = true
            try {
                val location = locationProvider.getCurrentLocation()
                if (location == null) {
                    message = "Couldn't get your location. Is device location turned on?"
                    return@launch
                }
                if (location.accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) {
                    message = "Location too imprecise (±${location.accuracy.toInt()} m). Try again near a window or outside."
                    return@launch
                }
                val updated = GymLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusMeters = gym?.radiusMeters ?: GymLocationStore.DEFAULT_RADIUS_METERS,
                )
                store.saveGym(updated)
                gym = updated
                geofenceManager.syncRegistration()
                message = "Gym location saved"
            } catch (e: SecurityException) {
                message = "Location permission is needed to set your gym"
            } finally {
                isLocating = false
            }
        }
    }

    fun setRadius(radiusMeters: Float) {
        val current = gym ?: return
        val updated = current.copy(radiusMeters = radiusMeters)
        store.saveGym(updated)
        gym = updated
        viewModelScope.launch { geofenceManager.syncRegistration() }
    }

    /** Called once every location permission is granted. */
    fun enableAutoCheckIn() {
        store.setAutoCheckInEnabled(true)
        autoCheckInEnabled = true
        viewModelScope.launch {
            if (!geofenceManager.syncRegistration()) {
                message = "Couldn't start auto check-in. Make sure device location is on."
            }
        }
    }

    fun disableAutoCheckIn() {
        store.setAutoCheckInEnabled(false)
        autoCheckInEnabled = false
        viewModelScope.launch { geofenceManager.unregister() }
    }

    fun manualCheckIn() {
        viewModelScope.launch {
            val result = repository.checkIn(VisitSource.MANUAL)
            _actionResults.tryEmit(result is CheckInResult.CheckedIn)
            message = when (result) {
                is CheckInResult.CheckedIn -> "Checked in"
                is CheckInResult.AlreadyCheckedIn -> "You're already checked in"
            }
        }
    }

    fun manualCheckOut() {
        viewModelScope.launch {
            val result = repository.checkOut()
            _actionResults.tryEmit(result !is CheckOutResult.NotCheckedIn)
            message = when (result) {
                is CheckOutResult.CheckedOut ->
                    "Checked out · ${formatDuration(result.visit.checkOutAt!! - result.visit.checkInAt)}"
                CheckOutResult.Discarded -> "Visit under 5 minutes discarded"
                CheckOutResult.NotCheckedIn -> "You're not checked in"
            }
        }
    }

    fun deleteVisit(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearMessage() {
        message = null
    }

    private companion object {
        const val MAX_ACCEPTABLE_ACCURACY_METERS = 100f
    }
}
