package com.nary.gym.data.remote.interceptors

import android.util.Log
import com.nary.gym.data.local.TokenManager
import com.nary.gym.data.remote.ApiService
import com.nary.gym.data.remote.dto.LogoutRequest
import com.nary.gym.data.session.UserSession
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val userSession: UserSession,
    @com.nary.gym.di.NoAuthApi private val apiServiceProvider: Provider<ApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        Log.d("TokenAuthenticator", "Authenticating: $path")

        if (response.responseCount > 2) {
            Log.d("TokenAuthenticator", "Response count > 2 for $path, giving up")
            return null
        }

        if (path.contains("auth/refresh-session")) {
            Log.d("TokenAuthenticator", "Refresh token call failed with 401, logging out")
            handleLogout()
            return null
        }

        synchronized(this) {
            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")

            Log.d("TokenAuthenticator", "Current: ${currentToken?.take(10)}, Request: ${requestToken?.take(10)}")

            if (currentToken != requestToken && currentToken != null) {
                Log.d("TokenAuthenticator", "Token already refreshed by another thread. Retrying.")
                return response.request.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                Log.d("TokenAuthenticator", "No refresh token available")
                return null
            }

            return try {
                Log.d("TokenAuthenticator", "Executing refresh call...")
                val refreshResponse = runBlocking {
                    apiServiceProvider.get().refreshToken(LogoutRequest(refreshToken))
                }

                if (refreshResponse.accessToken.isNotEmpty()) {
                    Log.d("TokenAuthenticator", "Success. New token: ${refreshResponse.accessToken.take(10)}")
                    tokenManager.saveTokens(
                        refreshResponse.accessToken,
                        refreshResponse.refreshToken
                    )
                    response.request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer ${refreshResponse.accessToken}")
                        .build()
                } else {
                    Log.d("TokenAuthenticator", "Empty access token received")
                    handleLogout()
                    null
                }
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Refresh exception", e)
                handleLogout()
                null
            }
        }
    }

    private fun handleLogout() {
        tokenManager.clearTokens()
        userSession.clearSession()
        runBlocking {
            userSession.emitUnauthorized()
        }
    }

    private val Response.responseCount: Int
        get() {
            var result = 1
            var response = priorResponse
            while (response != null) {
                result++
                response = response.priorResponse
            }
            return result
        }
}
