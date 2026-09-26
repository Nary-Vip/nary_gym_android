package com.nary.gym.testutil

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TestClock(var millis: Long = 0L, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = TestClock(millis, zone)
    override fun instant(): Instant = Instant.ofEpochMilli(millis)
    fun advance(duration: Duration) { millis += duration.toMillis() }
}
