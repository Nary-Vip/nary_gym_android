package com.nary.gym.viewModel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nary.gym.data.local.TokenManager
import com.nary.gym.data.session.UserSession
import com.nary.gym.repository.remote.AuthRepository
import com.nary.gym.utils.getErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object UpdateProfileLoading : ProfileUiState()
    object LogoutLoading : ProfileUiState()

    data class UpdateProfileSuccess(val message: String) : ProfileUiState()
    data class LogoutSuccess(val message: String) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userSession: UserSession,
    private val tokenManager: TokenManager,
    private val repository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Idle)
        private set

    var notificationsEnabled by mutableStateOf(tokenManager.getNotificationPreference())
        private set

    var reminderHour by mutableStateOf(tokenManager.getReminderHour())
        private set

    var reminderMinute by mutableStateOf(tokenManager.getReminderMinute())
        private set

    // Observe the current user from the shared session
    val currentUser = userSession.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val unauthorizedEvent = userSession.unauthorizedEvent

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        tokenManager.saveNotificationPreference(enabled)
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        reminderHour = hour
        reminderMinute = minute
        tokenManager.saveReminderTime(hour, minute)
    }

    fun hasToken(): Boolean = tokenManager.getAccessToken() != null

    fun updateProfile(name: String, phone: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                val current = currentUser.value
                val updatedName = if (name != current?.name) name else null
                val updatedPhone = if (phone != current?.phone) phone else null

                if (updatedName == null && updatedPhone == null && imageUri == null) {
                    uiState = ProfileUiState.Idle
                    return@launch
                }
                uiState = ProfileUiState.UpdateProfileLoading
                val updatedUser = repository.updateProfile(updatedName, updatedPhone, imageUri)
                userSession.setUser(updatedUser)
                uiState = ProfileUiState.UpdateProfileSuccess(message = "Profile Updated Successfully")
            } catch (e: Exception) {
                uiState = ProfileUiState.Error(message = e.getErrorMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            uiState = ProfileUiState.LogoutLoading
            try {
                repository.logout()
            } catch (e: Exception) {
                // Best-effort: the server call may fail (offline, already-expired token),
                // but the user must still be logged out locally.
            }
            userSession.clearSession()
            tokenManager.clearTokens()
            uiState = ProfileUiState.LogoutSuccess(message = "Logged out successfully")
            userSession.emitUnauthorized()
        }
    }

    fun resetUiState() {
        uiState = ProfileUiState.Idle
    }
}
