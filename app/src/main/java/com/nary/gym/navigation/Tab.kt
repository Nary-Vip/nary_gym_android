package com.nary.gym.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

enum class Tab(val label: String, val route: Any, val icon: ImageVector, val selectedIcon: ImageVector) {
    TODAY("Today", TodayRoute, Icons.Outlined.Today, Icons.Filled.Today),
    WORKOUTS("Workouts", WorkoutsRoute, Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    CHECK_INS("Check-ins", CheckInsRoute, Icons.Outlined.LocationOn, Icons.Filled.LocationOn),
}

fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
