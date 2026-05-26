package com.nary.gym.data.remote.dto

import com.google.gson.annotations.SerializedName

data class Workout(
    @SerializedName("_id")        val id: String,
    @SerializedName("workOutName") val workOutName: String,
    @SerializedName("workOutType") val workOutType: String,
    @SerializedName("workOutImage") val workOutImage: String
)

// GET /workouts response
data class WorkoutsResponse(
    @SerializedName("workouts")      val workouts: List<Workout>,
    @SerializedName("totalPages")    val totalPages: Int,
    @SerializedName("limit")         val limit: Int,
    @SerializedName("page")          val page: Int,
    @SerializedName("totalWorkouts") val totalWorkouts: Int
)

// POST / PUT response
data class WorkoutResponse(
    @SerializedName("message") val message: String,
    @SerializedName("workout") val workout: Workout
)

// DELETE response
data class MessageResponse(
    @SerializedName("message") val message: String
)