package com.nary.gym.data.session

import com.nary.gym.data.remote.dto.UserResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor() {
    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow();

    private val _unauthorizedEvent = MutableSharedFlow<Unit>(replay = 0)
    val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent.asSharedFlow()

    fun setUser(user: UserResponse){
        _currentUser.value = user
    }

    fun clearSession(){
        _currentUser.value = null
    }

    suspend fun emitUnauthorized() {
        _unauthorizedEvent.emit(Unit)
    }
}