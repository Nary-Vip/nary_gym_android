package com.nary.gym.utils

import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class VisitStatsTest {

    private val zone = ZoneOffset.UTC
    private fun at(y: Int, m: Int, d: Int, h: Int) =
        LocalDateTime.of(y, m, d, h, 0).toInstant(zone).toEpochMilli()
    private fun visit(checkIn: Long, minutes: Long?) = GymVisit(
        checkInAt = checkIn,
        checkOutAt = minutes?.let { checkIn + Duration.ofMinutes(it).toMillis() },
        source = VisitSource.AUTO,
    )

    @Test
    fun formatDuration_formatsMinutesAndHours() {
        assertEquals("0m", formatDuration(0))
        assertEquals("45m", formatDuration(Duration.ofMinutes(45).toMillis()))
        assertEquals("1h", formatDuration(Duration.ofMinutes(60).toMillis()))
        assertEquals("1h 12m", formatDuration(Duration.ofMinutes(72).toMillis()))
    }

    @Test
    fun weeklySummary_countsCompletedVisitsSinceMonday() {
        // 2026-09-24 is a Thursday; the week starts Monday 2026-09-21.
        val now = Instant.ofEpochMilli(at(2026, 9, 24, 20))
        val visits = listOf(
            visit(at(2026, 9, 21, 7), 60),   // Monday - counted
            visit(at(2026, 9, 23, 18), 30),  // Wednesday - counted
            visit(at(2026, 9, 20, 18), 90),  // last Sunday - excluded
            visit(at(2026, 9, 24, 19), null) // still open - excluded
        )

        val summary = weeklySummary(visits, now, zone)

        assertEquals(2, summary.visitCount)
        assertEquals(Duration.ofMinutes(90).toMillis(), summary.totalMillis)
    }

    @Test
    fun weeklySummary_onMonday_weekStartsAtThatMidnight() {
        // 2026-09-21 is a Monday: the week must start today at 00:00, not the Monday before.
        val now = Instant.ofEpochMilli(at(2026, 9, 21, 9))
        val visits = listOf(
            visit(at(2026, 9, 21, 0), 45),  // exactly Monday 00:00 - counted
            visit(at(2026, 9, 20, 23), 30)  // Sunday 23:00 - previous week, excluded
        )

        val summary = weeklySummary(visits, now, zone)

        assertEquals(1, summary.visitCount)
        assertEquals(Duration.ofMinutes(45).toMillis(), summary.totalMillis)
    }
}
