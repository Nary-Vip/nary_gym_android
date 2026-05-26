package com.nary.gym.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.ui.common_widgets.PasswordField
import com.nary.gym.ui.common_widgets.ProfileField
import com.nary.gym.ui.theme.Accent
import com.nary.gym.ui.theme.CardBg
import com.nary.gym.ui.theme.LabelColor
import com.nary.gym.ui.theme.Primary
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
                Toast.makeText(
                    context,
                    (uiState as LoginUiState.Error).message,
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
                .padding(top = 80.dp, bottom = 56.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NARY GYM",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Accent
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Welcome Back",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Log in to unleash the beast (hayaaa oh)",
                    fontSize = 13.sp,
                    color = Color(0xFF9090BB)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Sign In",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LabelColor,
                letterSpacing = 1.sp
            )

            ProfileField(
                label = "Email",
                value = formState.email,
                onValueChange = viewModel::onEmailChange,
                icon = Icons.Default.Email,
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
                error = formState.emailError
            )

            PasswordField(
                label = "Password",
                value = formState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Password@123",
                passwordVisible = formState.passwordVisible,
                onPasswordVisibleChange = viewModel::onTogglePasswordVisibility,
                error = formState.passwordError
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = uiState !is LoginUiState.Loading
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Log In",
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
                TextButton(onClick = onNavigateToCreateAccount) {
                    Text(
                        text = "Don't have an account? ",
                        color = LabelColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Sign Up",
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