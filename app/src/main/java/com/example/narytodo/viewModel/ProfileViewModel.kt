package com.example.narytodo.viewModel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.narytodo.data.local.TokenManager
import com.example.narytodo.data.session.UserSession
import com.example.narytodo.repository.remote.AuthRepository
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

    // Observe the current user from the shared session
    val currentUser = userSession.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val unauthorizedEvent = userSession.unauthorizedEvent

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
                uiState = ProfileUiState.Error(message = e.message ?: "Something Went Wrong")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                uiState = ProfileUiState.LogoutLoading
                repository.logout()
                userSession.clearSession()
                tokenManager.clearTokens()
                uiState = ProfileUiState.LogoutSuccess(message = "Logged out successfully")
                userSession.emitUnauthorized()
            } catch (e: Exception) {
                uiState = ProfileUiState.Error(message = e.message ?: "Something went wrong")
            }
        }
    }

    fun resetUiState() {
        uiState = ProfileUiState.Idle
    }
}
