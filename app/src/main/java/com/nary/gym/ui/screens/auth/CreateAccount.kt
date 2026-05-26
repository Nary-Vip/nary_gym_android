package com.nary.gym.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nary.gym.ui.common_widgets.PasswordField
import com.nary.gym.ui.common_widgets.ProfileField
import com.nary.gym.ui.theme.Accent
import com.nary.gym.ui.theme.CardBg
import com.nary.gym.ui.theme.LabelColor
import com.nary.gym.ui.theme.Primary
import com.nary.gym.ui.viewmodel.CreateAccountUiState
import com.nary.gym.ui.viewmodel.CreateAccountViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CreateAccountScreen(
    onNavigateToLogin: () -> Unit,
    onAccountCreated: () -> Unit,
    viewModel : CreateAccountViewModel = hiltViewModel()
) {

    val formState = viewModel.formState
    val uiState = viewModel.uiState
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onProfileImageSelected(uri)
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is CreateAccountUiState.Success -> {
                Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                viewModel.resetUiState()
                onAccountCreated()
            }

            is CreateAccountUiState.Error -> {
                print((uiState as CreateAccountUiState.Error).message)
                Toast.makeText(
                    context,
                    (uiState as CreateAccountUiState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetUiState()
            }

            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardBg)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Primary, Color(0xFF16213E))
                    )
                )
                .padding(top = 56.dp, bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NARY GYM",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Accent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create Account",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, Accent, CircleShape)
                            .background(Color(0xFF2A2A4A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.formState.profileImageUri != null) {
                            AsyncImage(
                                model = viewModel.formState.profileImageUri,
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile placeholder",
                                tint = Color(0xFF5A5A8A),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Accent)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Pick image",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upload a profile picture",
                    fontSize = 12.sp,
                    color = Color(0xFF9090BB)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Account Info",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LabelColor,
                letterSpacing = 1.sp,
            )

            ProfileField(
                label = "Full Name",
                value = viewModel.formState.name,
                onValueChange = viewModel::onNameChange,
                icon = Icons.Default.Person,
                placeholder = "Enter your name",
                keyboardType = KeyboardType.Text,
                error = formState.nameError,
            )

            ProfileField(
                label = "Email",
                value = viewModel.formState.email,
                onValueChange = viewModel::onEmailChange,
                icon = Icons.Default.Email,
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
                error = formState.emailError,
            )

            ProfileField(
                label = "Phone Number",
                value = viewModel.formState.phone,
                onValueChange = viewModel::onPhoneChange,
                icon = Icons.Default.Phone,
                placeholder = "+91 00000 00000",
                keyboardType = KeyboardType.Phone,
                error = formState.phoneError,
            )

            PasswordField(
                label = "Password",
                placeholder = "Password@123",
                value = viewModel.formState.password,
                onValueChange = viewModel::onPasswordChange,
                passwordVisible = viewModel.formState.passwordVisible,
                onPasswordVisibleChange = viewModel::onTogglePasswordVisibility,
                error = formState.passwordError,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = uiState !is CreateAccountUiState.Loading
            ) {
                if (uiState is CreateAccountUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onNavigateToLogin()}) {
                    Text(
                        text = "Already have an account? ",
                        color = LabelColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Log In",
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
