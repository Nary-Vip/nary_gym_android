package com.example.narytodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.narytodo.ui.screens.AllWorkOutListing
import com.example.narytodo.ui.theme.NaryToDoTheme
import com.example.narytodo.viewModel.TaskViewModel
import com.example.narytodo.viewModel.WorkOutViewModel
import com.example.narytodo.viewModel.WorkoutViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: WorkOutViewModel by viewModels {
        WorkoutViewModelFactory(application)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaryToDoTheme {
                AllWorkOutListing(viewModel)
            }
        }
    }
}
