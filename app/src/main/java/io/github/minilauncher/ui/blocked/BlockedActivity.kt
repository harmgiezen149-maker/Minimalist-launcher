package io.github.minilauncher.ui.blocked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import io.github.minilauncher.R
import io.github.minilauncher.blocking.BlockState
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.model.BlockReason
import io.github.minilauncher.data.model.BlockedInfo
import io.github.minilauncher.ui.common.AppLauncher
import io.github.minilauncher.ui.common.Async
import io.github.minilauncher.ui.common.BaseActivity
import io.github.minilauncher.ui.common.PermissionChecks
import io.github.minilauncher.usage.UsageRepository
import io.github.minilauncher.util.Time

/** Full-screen intervention shown instead of a blocked app. */
class BlockedActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }
        BlockState.clearFor(pkg)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: pkg
        val reason = intent.getStringExtra(EXTRA_REASON)?.let { BlockReason.valueOf(it) }
            ?: BlockReason.FOCUS_MODE
        val detail = intent.getStringExtra(EXTRA_DETAIL).orEmpty()

        findViewById<TextView>(R.id.blockedAppName).text =
            if (reason == BlockReason.WEBSITE) detail else label
        findViewById<TextView>(R.id.blockedReason).text = reasonText(reason, detail)
        showUsageToday(pkg, reason)
        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }
        setUpFiveMore(pkg, reason, detail)
    }

    private fun reasonText(reason: BlockReason, detail: String): String = when (reason) {
        BlockReason.FOCUS_MODE -> getString(R.string.blocked_reason_focus)
        BlockReason.SCHEDULE ->
            if (detail.isNotEmpty()) {
                getString(R.string.blocked_reason_schedule, detail)
            } else {
                getString(R.string.blocked_reason_schedule_generic)
            }
        BlockReason.LIMIT -> getString(R.string.blocked_reason_limit, detail)
        // MINDFUL normally routes to PauseActivity; defensive fallback text
        BlockReason.MINDFUL -> getString(R.string.blocked_reason_mindful)
        BlockReason.WEBSITE -> getString(R.string.blocked_reason_website)
    }

    /** Computed off the main thread: it walks every usage event since midnight. */
    private fun showUsageToday(pkg: String, reason: BlockReason) {
        val usageView = findViewById<TextView>(R.id.blockedUsage)
        if (reason == BlockReason.WEBSITE || !PermissionChecks.hasUsageAccess(this)) {
            usageView.visibility = View.GONE
            return
        }
        val usage = UsageRepository.get(this)
        Async.load(this, { usage.todayUsageMillis(pkg) / Time.MINUTE_MILLIS }) { minutes ->
            usageView.text = getString(R.string.blocked_usage_today, minutes)
        }
    }

    /**
     * The opt-in escape hatch. Not offered for the daily limit: that limit is
     * hard, and no allowance gets past it.
     */
    private fun setUpFiveMore(pkg: String, reason: BlockReason, detail: String) {
        val fiveMore = findViewById<TextView>(R.id.fiveMoreButton)
        val prefs = Prefs.get(this)
        if (!prefs.allowFiveMoreMinutes || reason == BlockReason.LIMIT) {
            fiveMore.visibility = View.GONE
            return
        }
        fiveMore.setOnClickListener {
            val until = System.currentTimeMillis() + FIVE_MORE_MINUTES * Time.MINUTE_MILLIS
            // Site allowances live under a "site:" key: colons are illegal in
            // package names, so this can never collide with app rules.
            prefs.allowTemporarily(if (reason == BlockReason.WEBSITE) "site:$detail" else pkg, until)
            AppLauncher.open(this, pkg)
            finish()
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_DETAIL = "detail"
        private const val FIVE_MORE_MINUTES = 5

        fun intent(context: Context, info: BlockedInfo, label: String): Intent =
            Intent(context, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_PACKAGE, info.packageName)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_REASON, info.reason.name)
                .putExtra(EXTRA_DETAIL, info.detail)
    }
}
