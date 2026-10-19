package com.nary.gym.utils

import com.nary.gym.core.WorkOutType

data class RotationDay(
    val dayType: WorkOutType,
    val dayNumber: Int,
    val dayCount: Int,
    val nextUp: WorkOutType,
    val completedToday: Boolean,
)

fun rotationDay(current: WorkOutType, completedToday: Boolean): RotationDay {
    val types = WorkOutType.entries
    val currentIndex = types.indexOf(current)
    val dayIndex = if (completedToday) (currentIndex - 1 + types.size) % types.size else currentIndex
    return RotationDay(
        dayType = types[dayIndex],
        dayNumber = dayIndex + 1,
        dayCount = types.size,
        nextUp = types[(dayIndex + 1) % types.size],
        completedToday = completedToday,
    )
}
