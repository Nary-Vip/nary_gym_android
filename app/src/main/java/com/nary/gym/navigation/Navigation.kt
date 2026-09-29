package com.nary.gym.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nary.gym.data.local.TokenManager
import com.nary.gym.ui.screens.CheckIn.CheckInScreen
import com.nary.gym.ui.screens.CreateAccountScreen
import com.nary.gym.ui.screens.Home.LegacyTodayTab
import com.nary.gym.ui.screens.Home.LegacyWorkoutsTab
import com.nary.gym.ui.screens.LoginScreen
import com.nary.gym.ui.screens.Profile.ProfileScreen
import com.nary.gym.viewModel.ProfileViewModel
import com.nary.gym.viewModel.WorkOutViewModel

@Composable
fun AppNavigator(
    navController: NavHostController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    tokenManager: TokenManager,
    currentIntent: Intent?
) {
    val context = LocalContext.current
    // A tab asked for by a notification tap; MainShell applies it and clears it.
    var requestedTab by remember { mutableStateOf<Tab?>(null) }

    LaunchedEffect(Unit) {
        profileViewModel.unauthorizedEvent.collect {
            val destination = navController.currentBackStackEntry?.destination
            val onAuthScreen = destination?.hasRoute(LoginRoute::class) == true ||
                destination?.hasRoute(CreateAccountRoute::class) == true
            if (!onAuthScreen) {
                navController.navigate(LoginRoute) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    // Keyed on currentIntent (Compose state on MainActivity) rather than activity.intent, since
    // onNewIntent's setIntent() is a plain field mutation Compose can't observe.
    LaunchedEffect(currentIntent) {
        val intent = currentIntent ?: return@LaunchedEffect
        val target = launchTarget(
            source = intent.getStringExtra("source"),
            dataUri = intent.data?.toString(),
            dataPath = intent.data?.path,
        )
        if (target == LaunchTarget.None) return@LaunchedEffect
        // Consume it so rotation/recomposition can't route again.
        (context as? Activity)?.intent?.apply { removeExtra("source"); data = null }
        if (tokenManager.getAccessToken() == null) return@LaunchedEffect // logged out: stay on Login
        when (target) {
            LaunchTarget.Profile -> navController.navigate(ProfileRoute) { launchSingleTop = true }
            is LaunchTarget.OpenTab -> requestedTab = target.tab
            LaunchTarget.None -> Unit
        }
    }

    val startDestination: Any = if (tokenManager.getAccessToken() != null) MainRoute else LoginRoute

    NavHost(navController = navController, startDestination = startDestination) {

        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(MainRoute) { popUpTo(LoginRoute) { inclusive = true } }
                },
                onNavigateToCreateAccount = { navController.navigate(CreateAccountRoute) }
            )
        }

        composable<CreateAccountRoute> {
            CreateAccountScreen(
                onAccountCreated = {
                    navController.navigate(MainRoute) { popUpTo(LoginRoute) { inclusive = true } }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable<MainRoute> {
            val workOutViewModel: WorkOutViewModel = hiltViewModel()
            val user by profileViewModel.currentUser.collectAsState()
            LaunchedEffect(Unit) { workOutViewModel.getCurrentUser() }

            MainShell(
                tabNavController = rememberNavController(),
                requestedTab = requestedTab,
                onRequestedTabHandled = { requestedTab = null },
                avatarName = user?.name,
                avatarImageUrl = user?.profile,
                onAvatarClick = { navController.navigate(ProfileRoute) { launchSingleTop = true } },
            ) { tab, padding ->
                when (tab) {
                    Tab.TODAY -> LegacyTodayTab(padding)
                    Tab.WORKOUTS -> LegacyWorkoutsTab(padding)
                    Tab.CHECK_INS -> CheckInScreen(padding)
                }
            }
        }

        composable<ProfileRoute> {
            ProfileRouteScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileRouteScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding -> ProfileScreen(padding) }
}
