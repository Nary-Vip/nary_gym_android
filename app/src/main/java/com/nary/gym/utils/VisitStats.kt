package com.nary.gym.utils

import com.nary.gym.data.room_db.GymVisit
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** "45m", "1h", "1h 12m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

data class WeeklySummary(val visitCount: Int, val totalMillis: Long)

/** Completed visits whose check-in falls in the current Monday-start week. */
fun weeklySummary(visits: List<GymVisit>, now: Instant, zone: ZoneId): WeeklySummary {
    val weekStart = now.atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    val completed = visits.filter { it.checkOutAt != null && it.checkInAt >= weekStart }
    return WeeklySummary(
        visitCount = completed.size,
        totalMillis = completed.sumOf { it.checkOutAt!! - it.checkInAt }
    )
}
