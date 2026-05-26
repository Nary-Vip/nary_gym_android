package com.nary.gym.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TaskViewModelFactory(private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TaskViewModel:: class.java)){
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        } else {
            throw IllegalArgumentException("unknown view model")
        }
    }
}
