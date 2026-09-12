package com.nary.gym.data.room_db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nary.gym.core.WorkOutType

@Entity(tableName = "workouts")
data class WorkOuts(
    @PrimaryKey
    val id: String,
    val workOutName: String,
    val workOutType: WorkOutType,
    val workOutImage: String,
)
