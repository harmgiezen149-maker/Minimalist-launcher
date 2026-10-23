package io.github.minilauncher.ui.common

import android.content.Context
import android.widget.Toast
import io.github.minilauncher.R
import io.github.minilauncher.blocking.BlockDecisionEngine
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.model.BlockReason
import io.github.minilauncher.data.model.BlockedInfo
import io.github.minilauncher.data.model.Decision
import io.github.minilauncher.ui.blocked.BlockedActivity
import io.github.minilauncher.ui.pause.PauseActivity
import io.github.minilauncher.usage.UsageRepository
import io.github.minilauncher.util.Time
import java.time.LocalDateTime

/**
 * Launches an app from the launcher UI, checking the block decision first so
 * the intervention screen appears immediately instead of after the
 * accessibility service catches up.
 */
object AppLauncher {

    /** Builds the engine Config from the current preferences. */
    fun configFrom(prefs: Prefs): BlockDecisionEngine.Config = BlockDecisionEngine.Config(
        blockedApps = prefs.blockedApps,
        focusModeEnabled = prefs.focusModeEnabled,
        schedules = prefs.schedules,
        limits = prefs.limits,
        tempAllowUntil = prefs.tempAllowUntil,
        mindfulApps = prefs.mindfulApps,
        focusSessionUntil = prefs.focusSessionUntil,
    )

    /** MINDFUL blocks get the pause screen; every other reason the block screen. */
    fun blockIntent(context: Context, info: BlockedInfo, label: String) = if (info.reason == BlockReason.MINDFUL) {
        PauseActivity.intent(context, info.packageName, label)
    } else {
        BlockedActivity.intent(context, info, label)
    }

    /**
     * Opens [pkg] or the screen that blocks it. An app with a daily limit
     * needs today's usage, which walks every usage event since midnight, so
     * that decision is made off the main thread; every other app opens
     * straight away.
     */
    fun launch(context: Context, pkg: String) {
        val app = context.applicationContext
        val needsUsage = pkg in Prefs.get(app).limits && PermissionChecks.hasUsageAccess(app)
        if (needsUsage) {
            Async.run({ decide(app, pkg) }) { decision -> act(app, pkg, decision) }
        } else {
            act(app, pkg, decide(app, pkg))
        }
    }

    /** Starts [pkg] without any block check, telling the user when it cannot be started. */
    fun open(context: Context, pkg: String) {
        if (AppRepository(context).launch(pkg)) return
        AppRepository.invalidate()
        Toast.makeText(context.applicationContext, R.string.app_not_found, Toast.LENGTH_SHORT).show()
    }

    private fun decide(context: Context, pkg: String): Decision {
        val dateTime = LocalDateTime.now()
        return BlockDecisionEngine.evaluate(
            packageName = pkg,
            config = configFrom(Prefs.get(context)),
            nowMillis = System.currentTimeMillis(),
            dayOfWeek = dateTime.dayOfWeek.value,
            minuteOfDay = dateTime.hour * Time.MINUTES_PER_HOUR + dateTime.minute,
            usageTodayMillis = {
                if (PermissionChecks.hasUsageAccess(context)) UsageRepository.get(context).todayUsageMillis(it) else 0L
            },
        )
    }

    private fun act(context: Context, pkg: String, decision: Decision) {
        when (decision) {
            is Decision.Allow -> open(context, pkg)
            is Decision.Block -> context.startActivity(
                blockIntent(
                    context,
                    BlockedInfo(pkg, decision.reason, decision.detail),
                    AppRepository(context).labelFor(pkg),
                ),
            )
        }
    }
}
