package io.github.minilauncher.blocking

import io.github.minilauncher.util.Time

/**
 * Tracks the continuous foreground session of the current app so the service
 * can post "you've been in X for N minutes" nudges. Purely in-memory.
 *
 * Thread-safe: the accessibility service uses it from its main thread
 * (screen-off, the minute tick) and from its worker thread (app switches).
 */
class SessionTracker(private val clock: () -> Long = { System.currentTimeMillis() }) {

    @get:Synchronized
    var currentPackage: String? = null
        private set

    private var sessionStart: Long = 0L
    private var nudgesFired: Int = 0

    /** Call on every foreground app change. Returns true if a new session started. */
    @Synchronized
    fun onForeground(pkg: String): Boolean {
        if (pkg == currentPackage) return false
        currentPackage = pkg
        sessionStart = clock()
        nudgesFired = 0
        return true
    }

    @Synchronized
    fun sessionMinutes(): Int {
        if (currentPackage == null) return 0
        return ((clock() - sessionStart) / Time.MINUTE_MILLIS).toInt()
    }

    /**
     * Returns the nudge milestone (in minutes) that should fire now, or null.
     * Fires at every multiple of [intervalMinutes] once per milestone.
     */
    @Synchronized
    fun dueNudgeMinutes(intervalMinutes: Int): Int? {
        if (intervalMinutes <= 0 || currentPackage == null) return null
        val milestone = sessionMinutes() / intervalMinutes
        if (milestone <= nudgesFired) return null
        nudgesFired = milestone
        return milestone * intervalMinutes
    }

    @Synchronized
    fun reset() {
        currentPackage = null
        sessionStart = 0L
        nudgesFired = 0
    }
}
