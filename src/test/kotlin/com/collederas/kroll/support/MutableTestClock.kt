package com.collederas.kroll.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableTestClock (
    private var instant: Instant,
    private val zone: ZoneId = ZoneOffset.UTC
) : Clock() {

    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock =
        MutableTestClock(instant, zone)

    override fun instant(): Instant = instant

    fun advanceBy(duration: Duration) {
        instant = instant.plus(duration)
    }
}

@TestConfiguration
class TestClockConfig {

    @Bean
    fun testClock(): MutableTestClock =
        MutableTestClock(Instant.parse("2026-01-01T00:00:00Z"))

    @Bean
    fun clock(testClock: MutableTestClock): Clock = testClock
}
