package io.github.minilauncher.blocking

import io.github.minilauncher.data.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StrictGuardTest {

    private val monday = 1
    private val now = 1_000_000L
    private val work = Schedule("s", "Work", setOf(monday), 9 * 60, 17 * 60, enabled = true)

    private fun lock(strict: Boolean = true, sessionUntil: Long = 0L, minuteOfDay: Int = 12 * 60) =
        StrictGuard.lock(strict, sessionUntil, listOf(work), now, monday, minuteOfDay)

    @Test
    fun `nothing is locked while strict mode is off`() {
        assertNull(lock(strict = false, sessionUntil = now + 60_000L))
    }

    @Test
    fun `a running focus session locks until it ends`() {
        assertEquals(StrictGuard.Lock.Session(now + 60_000L), lock(sessionUntil = now + 60_000L, minuteOfDay = 20 * 60))
    }

    @Test
    fun `an active schedule locks until its end`() {
        assertEquals(StrictGuard.Lock.ScheduleWindow(17 * 60), lock())
    }

    @Test
    fun `the session is reported before a schedule`() {
        assertEquals(StrictGuard.Lock.Session(now + 1L), lock(sessionUntil = now + 1L))
    }

    @Test
    fun `outside sessions and schedules nothing is locked`() {
        assertNull(lock(sessionUntil = now - 1L, minuteOfDay = 20 * 60))
    }
}
