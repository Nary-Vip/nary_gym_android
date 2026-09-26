package com.nary.gym.data.room_db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class VisitSource { AUTO, MANUAL }

@Entity(tableName = "gym_visits", indices = [Index("checkInAt")])
data class GymVisit(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val checkInAt: Long,
    val checkOutAt: Long? = null,
    val source: VisitSource,
    val autoClosed: Boolean = false,
)
