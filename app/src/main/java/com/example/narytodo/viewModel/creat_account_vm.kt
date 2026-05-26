package com.example.narytodo.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.narytodo.data.local.TokenManager
import com.example.narytodo.data.remote.dto.CreateAccountRequest
import com.example.narytodo.data.session.UserSession
import com.example.narytodo.repository.remote.AuthRepository
import com.example.narytodo.ui.utils.FieldValidators
import com.example.narytodo.utils.getErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateAccountFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val profileImageUri: Uri? = null,
    val passwordVisible: Boolean = false,

    // Field errors
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
)

sealed class CreateAccountUiState {
    object Idle : CreateAccountUiState()
    object Loading : CreateAccountUiState()
    data class Success(val message: String) : CreateAccountUiState()
    data class Error(val message: String) : CreateAccountUiState()
}

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val user: UserSession,
    private val tokenManager: TokenManager
) : ViewModel() {

    var formState by mutableStateOf(CreateAccountFormState())
        private set

    var uiState by mutableStateOf<CreateAccountUiState>(CreateAccountUiState.Idle)
        private set

    fun onNameChange(value: String) {
        formState = formState.copy(name = value, nameError = null)
    }

    fun onEmailChange(value: String) {
        formState = formState.copy(email = value, emailError = null)
    }

    fun onPhoneChange(value: String) {
        formState = formState.copy(phone = value, phoneError = null)
    }

    fun onPasswordChange(value: String) {
        formState = formState.copy(password = value, passwordError = null)
    }

    fun onProfileImageSelected(uri: Uri?) {
        formState = formState.copy(profileImageUri = uri)
    }

    fun onTogglePasswordVisibility() {
        formState = formState.copy(passwordVisible = !formState.passwordVisible)
    }

    private fun validateAll(): Boolean {
        val nameError     = FieldValidators.validateName(formState.name)
        val emailError    = FieldValidators.validateEmail(formState.email)
        val phoneError    = FieldValidators.validatePhoneNumber(formState.phone)
        val passwordError = FieldValidators.validatePassword(formState.password)

        formState = formState.copy(
            nameError     = nameError,
            emailError    = emailError,
            phoneError    = phoneError,
            passwordError = passwordError,
        )

        return listOf(nameError, emailError, phoneError, passwordError).all { it == null }
    }

    fun onSubmit() {
        if (!validateAll()) return

        viewModelScope.launch {
            uiState = CreateAccountUiState.Loading
            try {
                val response = repository.createAccount(
                    CreateAccountRequest(
                    name     = formState.name,
                    email    = formState.email,
                    phone    = formState.phone,
                    password = formState.password,
                    ),
                    imageUri = formState.profileImageUri
                )
                user.setUser(response.user)
                tokenManager.saveTokens(
                    accessToken = response.token.accessToken,
                    refreshToken = response.token.refreshToken
                )

                uiState = CreateAccountUiState.Success(response.user.name)
            } catch (e: Exception) {
                uiState = CreateAccountUiState.Error(e.getErrorMessage())
            }
        }
    }

    fun resetUiState() {
        uiState = CreateAccountUiState.Idle
    }
}