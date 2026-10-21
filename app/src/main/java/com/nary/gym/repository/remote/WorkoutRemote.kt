package com.nary.gym.repository.remote

import android.content.Context
import com.google.gson.JsonParser
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.remote.ApiService
import com.nary.gym.data.room_db.SyncState
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.utils.downloadImageToInternalStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.net.URLConnection
import javax.inject.Inject

interface WorkoutRemote {
    suspend fun create(name: String, type: WorkOutType, imagePath: String): String
    suspend fun update(serverId: String, name: String, type: WorkOutType, imagePath: String)
    suspend fun delete(serverId: String)
    suspend fun findIdByExactName(name: String): String?
    suspend fun fetchAll(): List<WorkOuts>
}

sealed class WorkoutRemoteException : Exception() {
    object NotFound : WorkoutRemoteException()
    object Conflict : WorkoutRemoteException()
    data class Rejected(override val message: String) : WorkoutRemoteException()
    data class Retryable(override val cause: Throwable?) : WorkoutRemoteException()
}

class RetrofitWorkoutRemote @Inject constructor(
    private val api: ApiService,
    @param:ApplicationContext private val context: Context,
) : WorkoutRemote {

    override suspend fun create(name: String, type: WorkOutType, imagePath: String): String = call {
        val image = imagePart(localImage(imagePath))
        api.createWorkout(name.textPart(), type.name.textPart(), image).workout.id
    }

    override suspend fun update(serverId: String, name: String, type: WorkOutType, imagePath: String) {
        call {
            val file = File(imagePath)
            api.updateWorkout(serverId, name.textPart(), type.name.textPart(), if (file.exists()) imagePart(file) else null)
        }
    }

    override suspend fun delete(serverId: String) {
        call { api.deleteWorkout(serverId) }
    }

    override suspend fun findIdByExactName(name: String): String? = call {
        api.getAllWorkOuts(page = 1, search = name, type = null, limit = 100)
            .workouts.firstOrNull { it.workOutName == name }?.id
    }

    override suspend fun fetchAll(): List<WorkOuts> = call {
        val all = mutableListOf<com.nary.gym.data.remote.dto.Workout>()
        var page = 1
        while (true) {
            val response = api.getAllWorkOuts(page = page, search = null, type = null, limit = 100)
            all += response.workouts
            if (response.workouts.isEmpty() || page >= response.totalPages) break
            page++
        }
        withContext(Dispatchers.IO) {
            all.map { work ->
                WorkOuts(
                    id = work.id,
                    workOutName = work.workOutName,
                    workOutType = WorkOutType.valueOf(work.workOutType.uppercase()),
                    workOutImage = downloadImageToInternalStorage(context, work.workOutImage),
                    serverId = work.id,
                    syncState = SyncState.SYNCED,
                )
            }
        }
    }

    private suspend fun localImage(imagePath: String): File {
        val path = if (imagePath.startsWith("http", ignoreCase = true)) {
            withContext(Dispatchers.IO) { downloadImageToInternalStorage(context, imagePath) }
        } else {
            imagePath
        }
        if (path.startsWith("http", ignoreCase = true)) throw WorkoutRemoteException.Retryable(null)
        val file = File(path)
        if (!file.exists()) throw WorkoutRemoteException.Rejected("Photo is missing — edit the workout to add one")
        return file
    }

    private fun imagePart(file: File): MultipartBody.Part {
        val mimeType = URLConnection.guessContentTypeFromName(file.name) ?: "image/jpeg"
        return MultipartBody.Part.createFormData("image", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
    }

    private fun String.textPart() = toRequestBody("text/plain".toMediaType())

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: WorkoutRemoteException) {
        throw e
    } catch (e: HttpException) {
        throw classify(e)
    } catch (e: IOException) {
        throw WorkoutRemoteException.Retryable(e)
    }

    private fun classify(e: HttpException): WorkoutRemoteException = when (val code = e.code()) {
        404 -> WorkoutRemoteException.NotFound
        409 -> WorkoutRemoteException.Conflict
        401, 429 -> WorkoutRemoteException.Retryable(e)
        in 400..499 -> WorkoutRemoteException.Rejected(serverMessage(e) ?: "Rejected ($code)")
        else -> WorkoutRemoteException.Retryable(e)
    }

    private fun serverMessage(e: HttpException): String? = runCatching {
        val json = JsonParser.parseString(e.response()?.errorBody()?.string().orEmpty()).asJsonObject
        json.get("message")?.asString
            ?: json.getAsJsonArray("errors")?.firstOrNull()?.asJsonObject?.get("message")?.asString
    }.getOrNull()
}
