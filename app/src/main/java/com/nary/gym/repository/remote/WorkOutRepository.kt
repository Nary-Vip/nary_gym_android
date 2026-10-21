package com.nary.gym.repository.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.remote.ApiService
import com.nary.gym.data.remote.dto.Workout
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.data.room_db.WorkOutsDao
import com.nary.gym.utils.downloadImageToInternalStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLConnection
import javax.inject.Inject

class WorkOutRepository @Inject constructor(
    private val dao: WorkOutsDao,
    private val api: ApiService,
    @param:ApplicationContext private val context: Context
) {
    fun getAllWorkouts(): Flow<List<WorkOuts>> {
        return dao.getAllWorkouts()
    }

    fun getWorkoutsByType(type: WorkOutType): Flow<List<WorkOuts>> {
        return dao.getWorkoutsByType(type)
    }

    suspend fun insert(workOuts: WorkOuts) {
        val response = api.createWorkout(
            workoutName = workOuts.workOutName.toRequestBody("text/plain".toMediaType()),
            workOutType = workOuts.workOutType.name.toRequestBody("text/plain".toMediaType()),
            image = if (workOuts.workOutImage.isNotBlank()) uriToMultipart(workOuts.workOutImage) else null
        )
        dao.insert(workOuts.copy(id = response.workout.id))
    }

    suspend fun update(workOuts: WorkOuts) {
        val response = api.updateWorkout(
            id = workOuts.id,
            workOutName = workOuts.workOutName.toRequestBody("text/plain".toMediaType()),
            workOutType = workOuts.workOutType.name.toRequestBody("text/plain".toMediaType()),
            image = if (workOuts.workOutImage.isNotBlank() && !workOuts.workOutImage.startsWith("http")) 
                uriToMultipart(workOuts.workOutImage) else null
        )
        dao.update(workOuts.copy(id = response.workout.id))
    }

    suspend fun delete(workOuts: WorkOuts) {
        api.deleteWorkout(workOuts.id)
        dao.delete(workOuts)
    }

    suspend fun syncWorkouts() {
        if (!isNetworkAvailable(context)) return

        val allWorkouts = fetchAllWorkoutPages()

        val workoutEntries = withContext(Dispatchers.IO) {
            allWorkouts.map { work ->
                WorkOuts(
                    id = work.id,
                    workOutName = work.workOutName,
                    workOutImage = downloadImageToInternalStorage(context, work.workOutImage),
                    workOutType = WorkOutType.valueOf(work.workOutType.uppercase()),
                    serverId = work.id,
                )
            }
        }

        dao.mergeFromServer(workoutEntries)
    }

    private suspend fun fetchAllWorkoutPages(): List<Workout> {
        val allWorkouts = mutableListOf<Workout>()
        var page = 1
        while (true) {
            val response = api.getAllWorkOuts(
                page = page,
                search = null,
                type = null,
                limit = 100
            )
            allWorkouts += response.workouts
            if (response.workouts.isEmpty() || page >= response.totalPages) break
            page++
        }
        return allWorkouts
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

    private fun uriToMultipart(filePath: String): MultipartBody.Part? {
        val file = File(filePath)
        if (!file.exists()) return null

        val mimeType = URLConnection.guessContentTypeFromName(file.name) ?: "image/jpeg"
        val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            "image",
            file.name,
            requestBody
        )
    }
}