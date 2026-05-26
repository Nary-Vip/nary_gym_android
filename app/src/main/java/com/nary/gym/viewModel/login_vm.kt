package com.nary.gym.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nary.gym.data.local.TokenManager
import com.nary.gym.data.session.UserSession
import com.nary.gym.repository.remote.AuthRepository
import com.nary.gym.ui.utils.FieldValidators
import com.nary.gym.utils.getErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
)

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val message: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val user: UserSession,
    private val tokenManager: TokenManager
) : ViewModel() {

    var formState by mutableStateOf(LoginFormState())
        private set

    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    fun onEmailChange(value: String) {
        formState = formState.copy(email = value, emailError = null)
    }

    fun onPasswordChange(value: String) {
        formState = formState.copy(password = value, passwordError = null)
    }

    fun onTogglePasswordVisibility() {
        formState = formState.copy(passwordVisible = !formState.passwordVisible)
    }

    private fun validateAll(): Boolean {
        val emailError    = FieldValidators.validateEmail(formState.email)
        val passwordError = FieldValidators.validatePassword(formState.password)

        formState = formState.copy(
            emailError    = emailError,
            passwordError = passwordError,
        )

        return emailError == null && passwordError == null
    }

    fun onSubmit() {
        if (!validateAll()) return

        viewModelScope.launch {
            uiState = LoginUiState.Loading
            try {
                val response = repository.login(
                    email    = formState.email,
                    password = formState.password
                )
                user.setUser(response.user)
                tokenManager.saveTokens(
                    accessToken = response.token.accessToken,
                    refreshToken = response.token.refreshToken
                )
                uiState = LoginUiState.Success(response.user.name)
            } catch (e: Exception) {
                uiState = LoginUiState.Error(e.getErrorMessage())
            }
        }
    }

    fun resetUiState() {
        uiState = LoginUiState.Idle
    }
}