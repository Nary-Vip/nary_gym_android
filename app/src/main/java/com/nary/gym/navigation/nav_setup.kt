package com.nary.gym.navigation

sealed class NavRoutes(val route: String) {
    object Login: NavRoutes("login")
    object CreateAccount: NavRoutes("create-account")

    object Home: NavRoutes("home")

}