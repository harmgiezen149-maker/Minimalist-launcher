package io.github.minilauncher.util

import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {

    @Test
    fun `report names time, version, thread and the exception with its cause`() {
        val error = IllegalStateException("outer", IllegalArgumentException("inner"))
        val report = CrashReport.format("2026-10-23 18:00:00", "1.42 (abc1234)", "main", error)
        assertTrue(report.startsWith("Time: 2026-10-23 18:00:00\nVersion: 1.42 (abc1234)\nThread: main\n"))
        assertTrue(report.contains("java.lang.IllegalStateException: outer"))
        assertTrue(report.contains("Caused by: java.lang.IllegalArgumentException: inner"))
    }

    @Test
    fun `very long reports are cut off`() {
        var error: Throwable = RuntimeException("root")
        repeat(2_000) { error = RuntimeException("level $it", error) }
        val report = CrashReport.format("t", "v", "main", error)
        assertTrue(report.length <= CrashReport.MAX_CHARS + 2)
        assertTrue(report.endsWith("…"))
    }
}
