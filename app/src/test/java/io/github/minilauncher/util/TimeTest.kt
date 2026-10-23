package io.github.minilauncher.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeTest {

    @Test
    fun `formats minute of day with leading zeros`() {
        assertEquals("00:00", Time.formatMinuteOfDay(0))
        assertEquals("09:05", Time.formatMinuteOfDay(9 * 60 + 5))
        assertEquals("23:59", Time.formatMinuteOfDay(Time.MINUTES_PER_DAY - 1))
    }

    @Test
    fun `minutes until rounds up`() {
        assertEquals(1, Time.minutesUntil(untilMillis = 30_000L, nowMillis = 0L))
        assertEquals(1, Time.minutesUntil(untilMillis = 60_000L, nowMillis = 0L))
        assertEquals(2, Time.minutesUntil(untilMillis = 60_001L, nowMillis = 0L))
        assertEquals(0, Time.minutesUntil(untilMillis = 0L, nowMillis = 0L))
    }
}
