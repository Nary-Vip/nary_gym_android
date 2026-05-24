package com.example.narytodo.data.session

import com.example.narytodo.data.remote.dto.TokenResponse
import com.example.narytodo.data.remote.dto.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor() {
    private val _currentUser = MutableStateFlow<UserResponse?>(null)

    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow();

    fun setUser(user: UserResponse){
        _currentUser.value = user
    }

    fun clearSession(){
        _currentUser.value = null
    }

    fun setTokens(token: TokenResponse){
//        _currentUser.value = user
    }

}