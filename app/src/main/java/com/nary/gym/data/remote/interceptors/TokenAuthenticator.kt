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
    private val apiServiceProvider: Provider<ApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "Authenticating: ${response.request.url}")
        if (response.responseCount > 2) {
            Log.d("TokenAuthenticator", "Response count > 2, giving up")
            return null
        }

        synchronized(this) {
            val path = response.request.url.encodedPath
            if (path.contains("auth/refresh-session")) {
                Log.d("TokenAuthenticator", "Refresh token call failed with 401, logging out")
                handleLogout()
                return null
            }

            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")

            Log.d("TokenAuthenticator", "Current token: ${currentToken?.take(10)}..., Request token: ${requestToken?.take(10)}...")

            if (currentToken != requestToken && currentToken != null) {
                Log.d("TokenAuthenticator", "Token already refreshed, retrying with new token")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                Log.d("TokenAuthenticator", "No refresh token available")
                return null
            }

            return try {
                Log.d("TokenAuthenticator", "Refreshing token...")
                val refreshResponse = runBlocking {
                    apiServiceProvider.get().refreshToken(LogoutRequest(refreshToken))
                }

                if (refreshResponse.accessToken.isNotEmpty()) {
                    Log.d("TokenAuthenticator", "Token refreshed successfully")
                    tokenManager.saveTokens(
                        refreshResponse.accessToken,
                        refreshResponse.refreshToken
                    )
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                        .build()
                } else {
                    Log.d("TokenAuthenticator", "Refresh response accessToken is empty")
                    handleLogout()
                    null
                }
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Refresh failed", e)
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
