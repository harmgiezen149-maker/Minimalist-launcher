package io.github.minilauncher.blocking

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTrackerTest {

    private var now = 0L
    private val tracker = SessionTracker { now }

    private fun advanceMinutes(minutes: Int) {
        now += minutes * 60_000L
    }

    @Test
    fun `a new app starts a session`() {
        assertTrue(tracker.onForeground("a"))
        assertEquals("a", tracker.currentPackage)
    }

    @Test
    fun `the same app again keeps the running session`() {
        tracker.onForeground("a")
        advanceMinutes(7)
        assertFalse(tracker.onForeground("a"))
        assertEquals(7, tracker.sessionMinutes())
    }

    @Test
    fun `switching apps restarts the clock`() {
        tracker.onForeground("a")
        advanceMinutes(7)
        tracker.onForeground("b")
        assertEquals(0, tracker.sessionMinutes())
    }

    @Test
    fun `no session means zero minutes and no nudge`() {
        assertEquals(0, tracker.sessionMinutes())
        assertNull(tracker.dueNudgeMinutes(5))
    }

    @Test
    fun `nudges fire once per milestone`() {
        tracker.onForeground("a")
        advanceMinutes(4)
        assertNull(tracker.dueNudgeMinutes(5))
        advanceMinutes(1)
        assertEquals(5, tracker.dueNudgeMinutes(5))
        assertNull(tracker.dueNudgeMinutes(5))
        advanceMinutes(5)
        assertEquals(10, tracker.dueNudgeMinutes(5))
    }

    @Test
    fun `a missed milestone fires the latest one only`() {
        tracker.onForeground("a")
        advanceMinutes(16)
        assertEquals(15, tracker.dueNudgeMinutes(5))
        assertNull(tracker.dueNudgeMinutes(5))
    }

    @Test
    fun `interval zero switches nudges off`() {
        tracker.onForeground("a")
        advanceMinutes(60)
        assertNull(tracker.dueNudgeMinutes(0))
    }

    @Test
    fun `reset ends the session`() {
        tracker.onForeground("a")
        advanceMinutes(10)
        tracker.reset()
        assertNull(tracker.currentPackage)
        assertNull(tracker.dueNudgeMinutes(5))
        assertTrue(tracker.onForeground("a"))
    }

    @Test
    fun `concurrent use from two threads stays consistent`() {
        val pool = Executors.newFixedThreadPool(2)
        repeat(2) { worker ->
            pool.execute {
                repeat(10_000) { i ->
                    if (worker == 0) tracker.onForeground(if (i % 2 == 0) "a" else "b") else tracker.reset()
                    tracker.dueNudgeMinutes(5)
                }
            }
        }
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
        tracker.reset()
        assertTrue(tracker.onForeground("c"))
    }
}
