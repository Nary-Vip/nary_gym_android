package com.nary.gym.data.room_db

import androidx.room.ColumnInfo
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
    val serverId: String? = null,
    @ColumnInfo(defaultValue = "SYNCED")
    val syncState: SyncState = SyncState.SYNCED,
    @ColumnInfo(defaultValue = "0")
    val version: Int = 0,
    val syncError: String? = null,
    val ownerId: String? = null,
)
