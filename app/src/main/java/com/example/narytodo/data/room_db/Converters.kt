package com.example.narytodo.data.room_db

import androidx.room.TypeConverter
import com.example.narytodo.core.WorkOutType

class Converters {

    @TypeConverter
    fun fromWorkoutType(value: WorkOutType): String {
        return value.name
    }

    @TypeConverter
    fun toWorkoutType(value: String): WorkOutType {
        return WorkOutType.valueOf(value)
    }
}