package com.example.narytodo.utils

import com.example.narytodo.data.remote.dto.MessageResponse
import com.google.gson.Gson
import retrofit2.HttpException

fun Throwable.getErrorMessage(): String {
    return when (this) {
        is HttpException -> {
            try {
                val errorBody = this.response()?.errorBody()?.string()
                val messageResponse = Gson().fromJson(errorBody, MessageResponse::class.java)
                messageResponse.message
            } catch (e: Exception) {
                this.message() ?: "An unknown error occurred"
            }
        }
        else -> this.message ?: "An unknown error occurred"
    }
}
