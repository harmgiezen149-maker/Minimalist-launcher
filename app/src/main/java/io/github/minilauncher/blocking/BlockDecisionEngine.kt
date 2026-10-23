package io.github.minilauncher.blocking

import io.github.minilauncher.data.model.BlockReason
import io.github.minilauncher.data.model.Decision
import io.github.minilauncher.data.model.Schedule
import io.github.minilauncher.util.Time

/**
 * Pure decision logic: given the current configuration and time, should a
 * package be blocked?
 *
 * The daily limit is hard: no temporary allowance (a mindful-pause duration
 * or "5 more minutes") lets an app past it. A live allowance does lift focus
 * mode, schedules and the mindful pause. When several rules block, focus mode
 * and schedules are reported first, then the limit, then the mindful pause.
 */
object BlockDecisionEngine {

    data class Config(
        val blockedApps: Set<String>,
        val focusModeEnabled: Boolean,
        val schedules: List<Schedule>,
        /** package -> daily limit in minutes */
        val limits: Map<String, Int>,
        /** package -> epoch millis until which the app is temporarily allowed */
        val tempAllowUntil: Map<String, Long>,
        /** apps behind the mindful pause: blocked until a usage duration is chosen */
        val mindfulApps: Set<String> = emptySet(),
        /** epoch millis until which a timed focus session is running (0 = none) */
        val focusSessionUntil: Long = 0L,
    )

    /**
     * @param usageTodayMillis lazily computed foreground time since local
     *   midnight; only invoked when the package has a limit configured.
     * @param dayOfWeek ISO day (1 = Monday .. 7 = Sunday)
     */
    fun evaluate(
        packageName: String,
        config: Config,
        nowMillis: Long,
        dayOfWeek: Int,
        minuteOfDay: Int,
        usageTodayMillis: (String) -> Long,
    ): Decision {
        val allowed = (config.tempAllowUntil[packageName] ?: 0L) > nowMillis
        val mindful = Decision.Block(BlockReason.MINDFUL, "").takeIf { !allowed && packageName in config.mindfulApps }
        return (if (allowed) null else blockListDecision(packageName, config, nowMillis, dayOfWeek, minuteOfDay))
            ?: limitDecision(packageName, config, usageTodayMillis)
            ?: mindful
            ?: Decision.Allow
    }

    /** Focus mode (manual or a timed session) or an active schedule, for apps on the block list. */
    private fun blockListDecision(
        packageName: String,
        config: Config,
        nowMillis: Long,
        dayOfWeek: Int,
        minuteOfDay: Int,
    ): Decision.Block? {
        if (packageName !in config.blockedApps) return null
        if (config.focusModeEnabled || nowMillis < config.focusSessionUntil) {
            return Decision.Block(BlockReason.FOCUS_MODE, "")
        }
        val active = ScheduleEvaluator.activeSchedule(config.schedules, dayOfWeek, minuteOfDay) ?: return null
        return Decision.Block(BlockReason.SCHEDULE, Time.formatMinuteOfDay(active.endMinute))
    }

    /** The daily limit; usage is only computed when the app has one. */
    private fun limitDecision(
        packageName: String,
        config: Config,
        usageTodayMillis: (String) -> Long,
    ): Decision.Block? {
        val limitMinutes = config.limits[packageName]?.takeIf { it > 0 } ?: return null
        val reached = usageTodayMillis(packageName) >= limitMinutes * Time.MINUTE_MILLIS
        return if (reached) Decision.Block(BlockReason.LIMIT, limitMinutes.toString()) else null
    }
}
