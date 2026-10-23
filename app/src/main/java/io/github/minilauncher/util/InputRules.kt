package io.github.minilauncher.util

/** Validation of numbers the user types in, kept pure so it is unit-tested. */
object InputRules {

    const val MIN_LIMIT_MINUTES = 1
    const val MAX_LIMIT_MINUTES = Time.MINUTES_PER_DAY

    /** A daily limit in minutes, or null when [text] is not a whole number from 1 to 1440. */
    fun parseLimitMinutes(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it in MIN_LIMIT_MINUTES..MAX_LIMIT_MINUTES }
}
