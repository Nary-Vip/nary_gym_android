package com.example.narytodo.repository

import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.room_db.TaskItem
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.data.room_db.WorkOutsDao
import kotlinx.coroutines.flow.Flow

class WorkOutRepository(private val dao: WorkOutsDao) {
    fun getAllWorkouts(): Flow<List<WorkOuts>> {
        return dao.getAllWorkouts()
    }

    fun getWorkoutsByType(type: WorkOutType): Flow<List<WorkOuts>> {
        return dao.getWorkoutsByType(type)
    }

    suspend fun insert(workOuts: WorkOuts){
        return dao.insert(workOuts)
    }

    suspend fun update(workOuts: WorkOuts){
        return dao.update(workOuts)
    }

    suspend fun delete(workOuts: WorkOuts){
        return dao.delete(workOuts)
    }
}
