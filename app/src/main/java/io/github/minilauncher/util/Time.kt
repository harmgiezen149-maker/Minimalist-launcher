package io.github.minilauncher.util

/** Time units and formatting shared by schedules, limits and the mode code. */
object Time {
    const val SECOND_MILLIS = 1_000L
    const val MINUTE_MILLIS = 60_000L
    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

    /** "09:05" for minute-of-day 545. */
    fun formatMinuteOfDay(minute: Int): String =
        "%02d:%02d".format(minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR)

    /**
     * Whole minutes from [nowMillis] until [untilMillis], rounded up, so a
     * countdown with 30 seconds left still reads "1 min" rather than "0 min".
     */
    fun minutesUntil(untilMillis: Long, nowMillis: Long): Int =
        ((untilMillis - nowMillis + MINUTE_MILLIS - 1) / MINUTE_MILLIS).toInt()
}
