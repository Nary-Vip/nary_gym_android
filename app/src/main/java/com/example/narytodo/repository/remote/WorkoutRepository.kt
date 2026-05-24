package com.example.narytodo.repository.remote

import android.content.Context
import android.net.Uri
import com.example.narytodo.data.remote.ApiService
import com.example.narytodo.data.remote.dto.MessageResponse
import com.example.narytodo.data.remote.dto.WorkoutResponse
import com.example.narytodo.data.remote.dto.WorkoutsResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class WorkoutRepository  @Inject constructor(
    private val api: ApiService,
    @param:ApplicationContext
    private val context: Context) {

    suspend fun getWorkouts(
        page: Int,
        limit: Int,
        search: String? = null,
        workoutType: String? = null
    ): WorkoutsResponse {
        return api.getAllWorkOuts(
            page = page,
            limit = limit,
            search = search,
            type = workoutType
        )
    }

    suspend fun createWorkout(
        workOutName: String,
        workOutType: String,
        imageUri: Uri
    ): WorkoutResponse {
        return api.createWorkout(
            workoutName = workOutName.toRequestBody("text/plain".toMediaType()),
            workOutType = workOutType.toRequestBody("text/plain".toMediaType()),
            image = uriToMultipart(imageUri)
        )
    }

    suspend fun updateWorkout(
        id: String,
        workOutName: String,
        workOutType: String,
        imageUri: Uri? = null
    ): WorkoutResponse {
        return api.updateWorkout(
            id = id,
            workOutName = workOutName.toRequestBody("text/plain".toMediaType()),
            workOutType = workOutType.toRequestBody("text/plain".toMediaType()),
            image = imageUri?.let { uriToMultipart(it) }
        )
    }

    suspend fun deleteWorkout(id: String): MessageResponse {
        return api.deleteWorkout(id)
    }

    private fun uriToMultipart(uri: Uri): MultipartBody.Part {
        val stream = context.contentResolver.openInputStream(uri)
        val bytes  = stream?.readBytes() ?: ByteArray(0)
        val body   = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "image.jpg", body)
    }
}