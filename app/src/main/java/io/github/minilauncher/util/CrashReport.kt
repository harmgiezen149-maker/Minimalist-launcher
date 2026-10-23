package io.github.minilauncher.util

/** The text of the local crash log. Pure, so it is unit-tested on a plain JVM. */
object CrashReport {

    /** Longest report kept: a deep stack overflow would otherwise fill megabytes. */
    const val MAX_CHARS = 20_000

    fun format(time: String, version: String, threadName: String, error: Throwable): String {
        val text = buildString {
            appendLine("Time: $time")
            appendLine("Version: $version")
            appendLine("Thread: $threadName")
            appendLine()
            append(error.stackTraceToString())
        }
        return if (text.length <= MAX_CHARS) text else text.take(MAX_CHARS) + "\n…"
    }
}
