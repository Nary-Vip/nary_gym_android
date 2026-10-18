package com.nary.gym.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nary.gym.ui.components.NaryTopAppBar
import kotlinx.coroutines.flow.first

@Composable
fun MainShell(
    tabNavController: NavHostController,
    requestedTab: Tab?,
    onRequestedTabHandled: () -> Unit,
    avatarName: String?,
    avatarImageUrl: String?,
    onAvatarClick: () -> Unit,
    tabContent: @Composable (tab: Tab, contentPadding: PaddingValues) -> Unit,
) {
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentTab = Tab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    } ?: Tab.TODAY

    Scaffold(
        topBar = {
            NaryTopAppBar(
                title = currentTab.label,
                avatarName = avatarName,
                avatarImageUrl = avatarImageUrl,
                onAvatarClick = onAvatarClick,
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tabNavController.navigateToTab(tab) },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = TodayRoute,
            enterTransition = { fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(90)) },
            popEnterTransition = { fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f) },
            popExitTransition = { fadeOut(tween(90)) },
        ) {
            composable<TodayRoute> { tabContent(Tab.TODAY, padding) }
            composable<WorkoutsRoute> { tabContent(Tab.WORKOUTS, padding) }
            composable<CheckInsRoute> { tabContent(Tab.CHECK_INS, padding) }
        }
    }

    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tabNavController.currentBackStackEntryFlow.first()
            tabNavController.navigateToTab(requestedTab)
            onRequestedTabHandled()
        }
    }
}
