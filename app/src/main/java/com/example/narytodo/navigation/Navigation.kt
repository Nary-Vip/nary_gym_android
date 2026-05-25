package com.example.narytodo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.narytodo.ui.screens.Home.AllWorkOutListing
import com.example.narytodo.ui.screens.CreateAccountScreen
import com.example.narytodo.ui.screens.LoginScreen
import com.example.narytodo.viewModel.ProfileViewModel


@Composable
fun AppNavigator(navController: NavHostController, profileViewModel: ProfileViewModel = hiltViewModel()) {
    val currentUser by profileViewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (currentUser != null) {
        NavRoutes.Home.route
    } else {
        NavRoutes.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToCreateAccount = {
                    navController.navigate(NavRoutes.CreateAccount.route)

                }
            )
        }

        composable(NavRoutes.CreateAccount.route) {
            CreateAccountScreen(
                onAccountCreated = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }

            )
        }
        composable(NavRoutes.Home.route) {
            AllWorkOutListing(navController = navController)
        }
    }

}