package com.nary.gym.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nary.gym.ui.components.NaryPasswordField
import com.nary.gym.ui.components.NaryTextField
import com.nary.gym.ui.viewmodel.CreateAccountUiState
import com.nary.gym.ui.viewmodel.CreateAccountViewModel

@Composable
fun CreateAccountScreen(
    onNavigateToLogin: () -> Unit,
    onAccountCreated: () -> Unit,
    viewModel: CreateAccountViewModel = hiltViewModel()
) {
    val formState = viewModel.formState
    val uiState = viewModel.uiState
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onProfileImageSelected(uri) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is CreateAccountUiState.Success -> {
                Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                viewModel.resetUiState()
                onAccountCreated()
            }
            is CreateAccountUiState.Error -> {
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
        AuthHeader(title = "Create account", subtitle = "Set up Nary Gym and start your rotation") {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (formState.profileImageUri != null) {
                            AsyncImage(
                                model = formState.profileImageUri,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FilledIconButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Add profile photo", modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("Add a profile photo (optional)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NaryTextField(formState.name, viewModel::onNameChange, "Full name", Icons.Filled.Person, error = formState.nameError)
            NaryTextField(formState.email, viewModel::onEmailChange, "Email", Icons.Filled.Email, keyboardType = KeyboardType.Email, error = formState.emailError, placeholder = "you@example.com")
            NaryTextField(formState.phone, viewModel::onPhoneChange, "Phone number", Icons.Filled.Phone, keyboardType = KeyboardType.Phone, error = formState.phoneError)
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
                enabled = uiState !is CreateAccountUiState.Loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (uiState is CreateAccountUiState.Loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create account")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onNavigateToLogin) { Text("Log in") }
            }
        }
    }
}
