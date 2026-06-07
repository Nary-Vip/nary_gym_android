package com.nary.gym.data.remote.interceptors

import com.nary.gym.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip adding token for auth endpoints
        if (path.contains("auth/login") || 
            path.contains("auth/create-account") || 
            path.contains("auth/refresh-session")) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenManager.getAccessToken()

        val request = if (accessToken != null) {
            originalRequest.newBuilder()
                .removeHeader("Authorization")
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        return response
    }
}
