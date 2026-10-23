package io.github.minilauncher.ui.stats

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.minilauncher.R
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.ui.common.Async
import io.github.minilauncher.ui.common.BaseActivity
import io.github.minilauncher.ui.common.PermissionChecks
import io.github.minilauncher.ui.onboarding.OnboardingActivity
import io.github.minilauncher.usage.UsageRepository
import io.github.minilauncher.util.Time

/** Today's screen time per app plus the filtered-notification summary. */
class StatsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    /** One computed line of the screen time list. */
    private data class UsageRow(val label: String, val minutes: Long)

    /**
     * Usage and labels come from slow system queries, so they are gathered
     * off the main thread and the list is filled in when they arrive.
     */
    private fun render() {
        val container = findViewById<LinearLayout>(R.id.statsContainer)
        if (!PermissionChecks.hasUsageAccess(this)) {
            container.removeAllViews()
            addRow(container, getString(R.string.stats_no_permission), bold = false)
            addRow(container, getString(R.string.stats_grant_permission), bold = true) {
                startActivity(android.content.Intent(this, OnboardingActivity::class.java))
            }
            renderFiltered(container, AppRepository(this))
            return
        }
        val repo = AppRepository(this)
        val usage = UsageRepository.get(this)
        Async.load(this, { usageRows(repo, usage) }) { rows ->
            container.removeAllViews()
            val totalMinutes = rows.sumOf { it.minutes }
            addRow(container, getString(R.string.stats_total, formatMinutes(totalMinutes)), bold = true)
            rows.filter { it.minutes > 0 }.forEach { row ->
                addRow(container, "${row.label} — ${formatMinutes(row.minutes)}", bold = false)
            }
            renderFiltered(container, repo)
        }
    }

    private fun usageRows(repo: AppRepository, usage: UsageRepository): List<UsageRow> {
        usage.invalidate()
        val launchable = repo.allApps(includeHidden = true, respectMode = false).associateBy { it.packageName }
        return usage.todayUsageByPackage()
            .filterKeys { it in launchable || it == packageName }
            .toList()
            .sortedByDescending { it.second }
            .map { (pkg, millis) ->
                UsageRow(launchable[pkg]?.displayLabel ?: repo.labelFor(pkg), millis / Time.MINUTE_MILLIS)
            }
    }

    private fun renderFiltered(container: LinearLayout, repo: AppRepository) {
        val filtered = Prefs.get(this).filteredCountsToday()
        if (filtered.isEmpty()) return
        addRow(container, "", bold = false)
        val total = filtered.values.sum()
        addRow(container, getString(R.string.stats_filtered_total, total), bold = true)
        filtered.toList().sortedByDescending { it.second }.forEach { (pkg, count) ->
            addRow(container, "${repo.labelFor(pkg)} — $count", bold = false)
        }
    }

    private fun formatMinutes(minutes: Long): String {
        val h = minutes / Time.MINUTES_PER_HOUR
        val m = minutes % Time.MINUTES_PER_HOUR
        return if (h > 0) {
            getString(R.string.stats_hours_minutes, h, m)
        } else {
            getString(R.string.stats_minutes, m)
        }
    }

    private fun addRow(
        container: LinearLayout,
        text: String,
        bold: Boolean,
        onClick: (() -> Unit)? = null,
    ) {
        val view = layoutInflater.inflate(R.layout.item_app_text, container, false) as TextView
        view.text = text
        if (bold) view.setTypeface(view.typeface, android.graphics.Typeface.BOLD)
        onClick?.let { handler -> view.setOnClickListener { handler() } }
        container.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        if (text.isEmpty()) view.visibility = View.INVISIBLE
    }
}
