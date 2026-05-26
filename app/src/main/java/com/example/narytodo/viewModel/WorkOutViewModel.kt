package com.example.narytodo.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.data.room_db.WorkoutDatabase
import com.example.narytodo.data.session.UserSession
import com.example.narytodo.repository.remote.AuthRepository
import com.example.narytodo.repository.remote.WorkOutRepository
import com.example.narytodo.utils.getErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkOutViewModel @Inject constructor(private val repository: WorkOutRepository, private  val authRepository: AuthRepository, val userSession: UserSession):
    ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val allWorkouts : StateFlow<List<WorkOuts>> = repository.getAllWorkouts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun getWorkOutByType(type: WorkOutType): StateFlow<List<WorkOuts>>{
        return repository.getWorkoutsByType(type).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
    }

    fun getCurrentUser(){
        viewModelScope.launch {
            errorMessage = null
            try {
                val response = authRepository.getUser()
                userSession.setUser(response.user)
            } catch (e: Exception) {
                errorMessage = e.getErrorMessage()
            }
        }
    }

    fun fetchWorkouts() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                repository.syncWorkouts()
            } catch (e: Exception) {
                errorMessage = e.getErrorMessage()
            } finally {
                isLoading = false
            }
        }
    }

    fun insert(workOuts: WorkOuts){
        viewModelScope.launch {
            errorMessage = null
            try {
                repository.insert(workOuts)
            } catch (e: Exception) {
                errorMessage = e.getErrorMessage()
            }
        }
    }

    fun delete(workOuts: WorkOuts){
        viewModelScope.launch {
            errorMessage = null
            try {
                repository.delete(workOuts)
            } catch (e: Exception) {
                errorMessage = e.getErrorMessage()
            }
        }
    }

    fun update(workOuts: WorkOuts){
        viewModelScope.launch {
            errorMessage = null
            try {
                repository.update(workOuts)
            } catch (e: Exception) {
                errorMessage = e.getErrorMessage()
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}