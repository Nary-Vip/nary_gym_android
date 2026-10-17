package com.nary.gym.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.ui.components.NaryPasswordField
import com.nary.gym.ui.components.NaryTextField
import com.nary.gym.ui.viewmodel.LoginUiState
import com.nary.gym.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToCreateAccount: () -> Unit
) {
    val formState = viewModel.formState
    val uiState = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> {
                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                viewModel.resetUiState()
                onLoginSuccess()
            }
            is LoginUiState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                viewModel.resetUiState()
            }
            else -> Unit
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        AuthHeader(title = "Welcome back", subtitle = "Log in to unleash the beast (hayaaa oh)")
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NaryTextField(
                value = formState.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                error = formState.emailError,
                placeholder = "you@example.com",
            )
            NaryPasswordField(
                value = formState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                visible = formState.passwordVisible,
                onToggleVisible = viewModel::onTogglePasswordVisibility,
                error = formState.passwordError,
            )
            Button(
                onClick = viewModel::onSubmit,
                enabled = uiState !is LoginUiState.Loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Log in")
                }
            }
            AuthSwitchPrompt("Don't have an account?", "Sign up", onNavigateToCreateAccount)
        }
    }
}
