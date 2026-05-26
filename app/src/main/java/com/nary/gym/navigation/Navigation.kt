package com.nary.gym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nary.gym.data.local.TokenManager
import com.nary.gym.ui.screens.Home.AllWorkOutListing
import com.nary.gym.ui.screens.CreateAccountScreen
import com.nary.gym.ui.screens.LoginScreen
import com.nary.gym.viewModel.ProfileViewModel


@Composable
fun AppNavigator(navController: NavHostController, profileViewModel: ProfileViewModel = hiltViewModel(), tokenManager: TokenManager) {
    LaunchedEffect(Unit) {
        profileViewModel.unauthorizedEvent.collect {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != NavRoutes.Login.route && currentRoute != NavRoutes.CreateAccount.route) {
                navController.navigate(NavRoutes.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val startDestination = if (tokenManager.getAccessToken() != null) {
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