package com.nary.gym.data.room_db

import androidx.room.TypeConverter
import com.nary.gym.core.WorkOutType

class Converters {

    @TypeConverter
    fun fromWorkoutType(value: WorkOutType): String {
        return value.name
    }

    @TypeConverter
    fun toWorkoutType(value: String): WorkOutType {
        return WorkOutType.valueOf(value)
    }

    @TypeConverter
    fun fromVisitSource(value: VisitSource): String = value.name

    @TypeConverter
    fun toVisitSource(value: String): VisitSource = VisitSource.valueOf(value)

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}
