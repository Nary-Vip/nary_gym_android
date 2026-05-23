package com.example.narytodo.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.data.room_db.WorkoutDatabase
import com.example.narytodo.repository.WorkOutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkOutViewModel @Inject constructor(private val repository: WorkOutRepository):
    ViewModel() {

    val allWorkouts : StateFlow<List<WorkOuts>> = repository.getAllWorkouts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun getWorkOutByType(type: WorkOutType): StateFlow<List<WorkOuts>>{
        return repository.getWorkoutsByType(type).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
    }

    fun insert(workOuts: WorkOuts){
        viewModelScope.launch {
            repository.insert(workOuts)
        }
    }

    fun delete(workOuts: WorkOuts){
        viewModelScope.launch {
            repository.delete(workOuts)
        }
    }

    fun update(workOuts: WorkOuts){
        viewModelScope.launch {
            repository.update(workOuts)
        }
    }
}