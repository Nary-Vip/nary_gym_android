package com.example.narytodo.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.data.room_db.WorkoutDatabase
import com.example.narytodo.repository.WorkOutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkOutViewModel(application: Application): AndroidViewModel(application) {
    private val dao = WorkoutDatabase.getDatabase(application).workoutDao()

    private val repository = WorkOutRepository(dao)

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