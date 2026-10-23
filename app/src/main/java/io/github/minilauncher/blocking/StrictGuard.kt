package io.github.minilauncher.blocking

import io.github.minilauncher.data.model.Schedule

/**
 * Strict mode: while a timed focus session or a blocking schedule runs,
 * nothing that loosens blocking can be changed (unblocking, raising or
 * removing a limit, cancelling the session, switching a schedule off,
 * "5 more minutes"). Manual focus mode does not lock anything: it has no end
 * of its own, so locking it would lock it forever. Pure, so it is unit-tested.
 */
object StrictGuard {

    sealed class Lock {
        /** A focus session runs until this moment. */
        data class Session(val untilMillis: Long) : Lock()

        /** A blocking schedule runs until this minute of the day. */
        data class ScheduleWindow(val endMinute: Int) : Lock()
    }

    /** What currently prevents loosening, or null when nothing does. */
    fun lock(
        strictMode: Boolean,
        focusSessionUntil: Long,
        schedules: List<Schedule>,
        nowMillis: Long,
        dayOfWeek: Int,
        minuteOfDay: Int,
    ): Lock? {
        if (!strictMode) return null
        if (nowMillis < focusSessionUntil) return Lock.Session(focusSessionUntil)
        val active = ScheduleEvaluator.activeSchedule(schedules, dayOfWeek, minuteOfDay) ?: return null
        return Lock.ScheduleWindow(active.endMinute)
    }
}
