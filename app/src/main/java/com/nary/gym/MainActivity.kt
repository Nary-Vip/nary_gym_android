package com.nary.gym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.nary.gym.data.local.TokenManager
import com.nary.gym.navigation.AppNavigator
import com.nary.gym.ui.theme.NaryToDoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaryToDoTheme {
                val navController = rememberNavController()
                AppNavigator(navController, tokenManager =  tokenManager)
            }
        }
    }
}
