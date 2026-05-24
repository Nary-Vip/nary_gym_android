package com.example.narytodo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.narytodo.ui.screens.AllWorkOutListing
import com.example.narytodo.ui.screens.CreateAccountScreen
import com.example.narytodo.ui.screens.LoginScreen


@Composable
fun AppNavigator(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.Login.route) {

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