package com.example.narytodo.data.remote.dto

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
    val profile: String
)