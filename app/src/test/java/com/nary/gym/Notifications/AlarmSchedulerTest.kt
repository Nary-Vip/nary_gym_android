package com.nary.gym.Notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmSchedulerTest {

    private val india = ZoneId.of("Asia/Kolkata")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: ZoneId = india) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun beforeTheReminderTime_firesLaterToday() {
        assertEquals(at(2026, 10, 19, 7, 30), AlarmScheduler.nextDailyTrigger(at(2026, 10, 19, 6, 0), india, 7, 30))
    }

    @Test
    fun afterTheReminderTime_firesTomorrow() {
        assertEquals(at(2026, 10, 20, 7, 30), AlarmScheduler.nextDailyTrigger(at(2026, 10, 19, 8, 0), india, 7, 30))
    }

    @Test
    fun exactlyAtTheReminderTime_firesTomorrow() {
        assertEquals(at(2026, 10, 20, 7, 30), AlarmScheduler.nextDailyTrigger(at(2026, 10, 19, 7, 30), india, 7, 30))
    }

    @Test
    fun reminderTimeInADaylightSavingGap_movesForwardToTheNextValidTime() {
        val london = ZoneId.of("Europe/London")
        assertEquals(
            at(2026, 3, 29, 2, 30, london),
            AlarmScheduler.nextDailyTrigger(at(2026, 3, 28, 23, 0, london), london, 1, 30),
        )
    }
}
