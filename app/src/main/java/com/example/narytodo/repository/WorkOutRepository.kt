package com.example.narytodo.repository

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import com.example.narytodo.core.WorkOutType
import com.example.narytodo.data.remote.ApiService
import com.example.narytodo.data.room_db.TaskItem
import com.example.narytodo.data.room_db.WorkOuts
import com.example.narytodo.data.room_db.WorkOutsDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkOutRepository @Inject constructor(
    private val dao: WorkOutsDao,
    private val api: ApiService,
    @ApplicationContext private val context: Context
) {
    fun getAllWorkouts(): Flow<List<WorkOuts>> {
        return dao.getAllWorkouts()
    }

    fun getWorkoutsByType(type: WorkOutType): Flow<List<WorkOuts>> {
        return dao.getWorkoutsByType(type)
    }

    suspend fun insert(workOuts: WorkOuts) {
        return dao.insert(workOuts)
    }

    suspend fun update(workOuts: WorkOuts) {
        return dao.update(workOuts)
    }

    suspend fun delete(workOuts: WorkOuts) {
        return dao.delete(workOuts)
    }
    suspend fun syncWorkouts() {
        if(isNetworkAvailable(context)){
            try {
                val response = api.getAllWorkOuts(
                    page = 1,
                    search = null,
                    type = null,
                    limit = 100
                )

                val workoutEntries = response.workouts.map { work ->
                    WorkOuts(
                        id = work.id,
                        workOutName = work.workOutName,
                        workOutImage = work.workOutImage,
                        workOutType = WorkOutType.valueOf(work.workOutType.uppercase()),
                    )
                }

                dao.insertAllWorkouts(workoutEntries)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
