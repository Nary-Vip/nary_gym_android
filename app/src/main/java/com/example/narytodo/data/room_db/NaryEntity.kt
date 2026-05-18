package com.example.narytodo.data.room_db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.narytodo.core.WorkOutType

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val isDone: Boolean = false)


@Entity(tableName = "workouts")
data class WorkOuts(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workOutName: String,
    val workOutType: WorkOutType,
    val workOutImage: String,
    val dayId: Int)


@Entity(tableName = "workoutdays")
data class WorkOutsDay(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workOutType: WorkOutType)


data class WorkoutDayRelation(
    @Embedded
    val workoutDay: WorkOutsDay,

    @Relation(parentColumn = "id", entityColumn = "dayId")
    val workouts: List<WorkOuts>
)