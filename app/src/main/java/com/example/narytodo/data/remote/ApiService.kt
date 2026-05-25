package com.example.narytodo.data.remote

import com.example.narytodo.data.remote.dto.AuthResponse
import com.example.narytodo.data.remote.dto.LoginRequest
import com.example.narytodo.data.remote.dto.LogoutRequest
import com.example.narytodo.data.remote.dto.MessageResponse
import com.example.narytodo.data.remote.dto.UpdateProfileResponse
import com.example.narytodo.data.remote.dto.UserResponse
import com.example.narytodo.data.remote.dto.WorkoutResponse
import com.example.narytodo.data.remote.dto.WorkoutsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface ApiService {
    @Multipart
    @POST("auth/create-account")
    suspend fun createAccount(
        @Part image: MultipartBody.Part?,
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("password") password: RequestBody
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @GET("auth/get-user")
    suspend fun getUser(): AuthResponse

    @Multipart
    @PATCH("auth/update-account")
    suspend fun updateProfile(
        @Part image: MultipartBody.Part?,
        @Part("name") name: RequestBody?,
        @Part("phone") phone: RequestBody?
    ): UpdateProfileResponse


    @GET("workouts")
    suspend fun getAllWorkOuts(
        @Query("page") page: Int,
        @Query("search") search: String?,
        @Query("type") type: String?,
        @Query("limit") limit: Int
    ): WorkoutsResponse


    @Multipart
    @POST("workouts")
    suspend fun createWorkout(
        @Part("workOutName") workoutName: RequestBody,
        @Part("workOutType") workOutType: RequestBody,
        @Part image: MultipartBody.Part?
    ): WorkoutResponse

    @Multipart
    @PUT("workouts/{id}")
    suspend fun updateWorkout(
        @Path("id")          id:          String,
        @Part("workOutName") workOutName: RequestBody,
        @Part("workOutType") workOutType: RequestBody,
        @Part image:         MultipartBody.Part?
    ): WorkoutResponse

    @DELETE("workouts/{id}")
    suspend fun deleteWorkout(
        @Path("id") id: String
    ): MessageResponse


    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): MessageResponse
}