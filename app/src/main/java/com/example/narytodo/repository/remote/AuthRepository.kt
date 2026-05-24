package com.example.narytodo.repository.remote

import android.content.Context
import android.net.Uri
import com.example.narytodo.data.local.TokenManager
import com.example.narytodo.data.remote.ApiService
import com.example.narytodo.data.remote.dto.AuthResponse
import com.example.narytodo.data.remote.dto.CreateAccountRequest
import com.example.narytodo.data.remote.dto.LoginRequest
import com.example.narytodo.data.remote.dto.LogoutRequest
import com.example.narytodo.data.remote.dto.MessageResponse
import com.example.narytodo.data.remote.dto.UserResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: ApiService,
    @param:ApplicationContext
    private val context: Context,
    private val tokenManager: TokenManager,
    ) {

    suspend fun login (email: String, password: String): AuthResponse{
        return api.login(LoginRequest(email, password))
    }

    suspend fun createAccount (request: CreateAccountRequest, imageUri: Uri? ) : AuthResponse {
        val imagePart = imageUri?.let { uri ->
            val stream = context.contentResolver.openInputStream(uri)
            val bytes  = stream?.readBytes() ?: return@let null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val body   = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", "profile.jpg", body)
        }

        return api.createAccount(imagePart, name = request.name.toRequestBody("text/plain".toMediaTypeOrNull()) , email =request.email.toRequestBody("text/plain".toMediaTypeOrNull()) , phone = request.phone.toRequestBody("text/plain".toMediaTypeOrNull()) , password = request.password.toRequestBody("text/plain".toMediaTypeOrNull()))
    }

    suspend fun updateProfile(name: String?, phone: String?, imageUri: Uri?): UserResponse {
        val imagePart = imageUri?.let { uri ->
            val stream = context.contentResolver.openInputStream(uri)
            val bytes = stream?.readBytes() ?: return@let null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", "profile.jpg", body)
        }

        return api.updateProfile(
            image = imagePart,
            name = name?.toRequestBody("text/plain".toMediaTypeOrNull()),
            phone = phone?.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    suspend fun logout (): MessageResponse{
        val token = tokenManager.getRefreshToken()
        return api.logout(LogoutRequest(refreshToken =  token ?: ""))
    }
}
