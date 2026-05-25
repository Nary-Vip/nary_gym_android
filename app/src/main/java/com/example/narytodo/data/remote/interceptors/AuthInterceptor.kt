package com.example.narytodo.data.remote.interceptors

import com.example.narytodo.data.local.TokenManager
import com.example.narytodo.data.session.UserSession
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val userSession: UserSession
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenManager.getAccessToken()

        val request = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        if (response.code == 401) {
            // Clear session
            tokenManager.clearTokens()
            userSession.clearSession()
        }

        return response
    }
}
