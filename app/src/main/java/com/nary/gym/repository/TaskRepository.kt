package com.nary.gym.repository

import com.nary.gym.data.room_db.TaskDao
import com.nary.gym.data.room_db.TaskItem
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun getAllTasks (): Flow<List<TaskItem>> {
        return dao.getAllTasks()
    }

    suspend fun insert(taskItem: TaskItem){
        return dao.insert(taskItem)
    }

    suspend fun update(taskItem: TaskItem){
        return dao.update(taskItem)
    }

    suspend fun delete(taskItem: TaskItem){
        return dao.delete(taskItem)
    }

}