package com.example.narytodo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class CreateAccountRequest(
    val email: String,
    val password: String,
    val phone: String,
    val name: String,
)

data class AuthResponse(
    val token: TokenResponse,
    val user: UserResponse
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val phone: String,
    val name: String,
    @SerializedName("profileImage")
    val profile: String?
)

data class UpdateProfileResponse(
    val user: UserResponse
)

data class LogoutRequest(
    val refreshToken: String,
)
