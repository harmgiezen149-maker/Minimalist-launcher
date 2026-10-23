package io.github.minilauncher.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import io.github.minilauncher.App
import io.github.minilauncher.BuildConfig
import io.github.minilauncher.R
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.CrashLog
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.ui.common.AppLongPressDialog
import io.github.minilauncher.ui.common.BaseActivity
import io.github.minilauncher.ui.common.EveningOverrideDialog
import io.github.minilauncher.ui.common.PermissionChecks
import io.github.minilauncher.ui.common.StrictModeGate
import io.github.minilauncher.ui.onboarding.OnboardingActivity
import io.github.minilauncher.ui.stats.StatsActivity
import io.github.minilauncher.util.Time

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: Prefs
    private val appLists by lazy { AppListDialogs(this) { render() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs.get(this)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val container = findViewById<LinearLayout>(R.id.settingsContainer)
        container.removeAllViews()
        renderFocus(container)
        renderAllowances(container)
        renderMode(container)
        renderNotifications(container)
        renderAppearance(container)
        renderLauncher(container)
        renderBackup(container)
        renderAbout(container)
    }

    /** Focus mode, block lists, sessions, schedules and limits. */
    private fun renderFocus(container: LinearLayout) {
        section(container, getString(R.string.settings_section_focus))
        row(
            container,
            getString(R.string.settings_focus_mode),
            onOff(prefs.focusModeEnabled),
        ) {
            prefs.focusModeEnabled = !prefs.focusModeEnabled
            render()
        }
        row(container, getString(R.string.settings_blocked_apps), prefs.blockedApps.size.toString()) {
            appLists.showMultiPicker(
                title = getString(R.string.settings_blocked_apps),
                selected = prefs.blockedApps,
            ) { chosen -> applyBlockList(prefs.blockedApps, chosen) { prefs.blockedApps = it } }
        }
        row(container, getString(R.string.settings_mindful_apps), prefs.mindfulApps.size.toString()) {
            appLists.showMultiPicker(
                title = getString(R.string.settings_mindful_apps),
                selected = prefs.mindfulApps,
            ) { chosen -> applyBlockList(prefs.mindfulApps, chosen) { prefs.mindfulApps = it } }
        }
        focusSessionRow(container)
        row(container, getString(R.string.settings_blocked_sites), prefs.blockedSites.size.toString()) {
            showBlockedSites()
        }
        row(container, getString(R.string.settings_schedules), prefs.schedules.size.toString()) {
            startActivity(Intent(this, ScheduleEditorActivity::class.java))
        }
        row(container, getString(R.string.settings_limits), prefs.limits.size.toString()) {
            showLimitsOverview()
        }
    }

    /** The timed focus session: start one, or see and end the running one. */
    private fun focusSessionRow(container: LinearLayout) {
        val sessionActive = prefs.focusSessionUntil > System.currentTimeMillis()
        row(
            container,
            getString(R.string.settings_focus_session),
            if (sessionActive) {
                getString(
                    R.string.focus_session_remaining,
                    Time.minutesUntil(prefs.focusSessionUntil, System.currentTimeMillis()),
                )
            } else {
                getString(R.string.state_off)
            },
        ) {
            if (sessionActive) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.focus_session_cancel_title)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        StrictModeGate.loosen(this) {
                            prefs.focusSessionUntil = 0L
                            render()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                showFocusSessionPicker()
            }
        }
    }

    /** Saves a new block or mindful list; taking apps off it counts as loosening. */
    private fun applyBlockList(current: Set<String>, chosen: Set<String>, save: (Set<String>) -> Unit) {
        val apply = {
            save(chosen)
            render()
        }
        if (chosen.containsAll(current)) apply() else StrictModeGate.loosen(this, apply)
    }

    /** Escape hatches and reminders, still under the focus header. */
    private fun renderAllowances(container: LinearLayout) {
        row(
            container,
            getString(R.string.settings_five_more),
            onOff(prefs.allowFiveMoreMinutes),
        ) {
            if (prefs.allowFiveMoreMinutes) {
                prefs.allowFiveMoreMinutes = false
                render()
            } else {
                StrictModeGate.loosen(this) {
                    prefs.allowFiveMoreMinutes = true
                    render()
                }
            }
        }
        row(container, getString(R.string.settings_strict_mode), onOff(prefs.strictMode)) {
            if (prefs.strictMode) {
                StrictModeGate.loosen(this) {
                    prefs.strictMode = false
                    render()
                }
            } else {
                prefs.strictMode = true
                render()
            }
        }
        row(
            container,
            getString(R.string.settings_nudge),
            if (prefs.nudgeIntervalMinutes == 0) {
                getString(R.string.state_off)
            } else {
                getString(R.string.settings_nudge_value, prefs.nudgeIntervalMinutes)
            },
        ) { showNudgePicker() }
        row(
            container,
            getString(R.string.settings_nudge_exempt),
            prefs.nudgeExemptApps.size.toString(),
        ) {
            appLists.showMultiPicker(
                title = getString(R.string.settings_nudge_exempt),
                selected = prefs.nudgeExemptApps,
            ) {
                prefs.nudgeExemptApps = it
                render()
            }
        }
    }

    /** Day/evening mode; the details only while it is on. */
    private fun renderMode(container: LinearLayout) {
        section(container, getString(R.string.settings_section_mode))
        row(
            container,
            getString(R.string.settings_mode_enabled),
            onOff(prefs.dayEveningEnabled),
        ) {
            prefs.dayEveningEnabled = !prefs.dayEveningEnabled
            render()
        }
        if (prefs.dayEveningEnabled) {
            row(
                container,
                getString(R.string.settings_evening_start),
                Time.formatMinuteOfDay(prefs.eveningStartMinute),
            ) {
                pickTime(prefs.eveningStartMinute) {
                    prefs.eveningStartMinute = it
                    render()
                }
            }
            row(
                container,
                getString(R.string.settings_evening_end),
                Time.formatMinuteOfDay(prefs.eveningEndMinute),
            ) {
                pickTime(prefs.eveningEndMinute) {
                    prefs.eveningEndMinute = it
                    render()
                }
            }
            row(
                container,
                getString(R.string.settings_app_visibility),
                prefs.appVisibility.size.toString(),
            ) { appLists.showVisibilityList() }
            row(
                container,
                getString(R.string.settings_notification_windows),
                prefs.notificationWindows.size.toString(),
            ) { appLists.showNotificationWindowList() }
            val overrideActive = prefs.eveningOverrideUntil > System.currentTimeMillis()
            row(
                container,
                getString(R.string.settings_evening_override),
                if (overrideActive) {
                    getString(
                        R.string.focus_session_remaining,
                        prefs.currentModeState().overrideRemainingMinutes,
                    )
                } else {
                    getString(R.string.state_off)
                },
            ) { EveningOverrideDialog.show(this) { render() } }
        }
    }

    /** The notification filter. */
    private fun renderNotifications(container: LinearLayout) {
        section(container, getString(R.string.settings_section_notifications))
        row(container, getString(R.string.settings_muted_apps), prefs.mutedApps.size.toString()) {
            if (!PermissionChecks.isNotificationListenerEnabled(this)) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } else {
                appLists.showMultiPicker(
                    title = getString(R.string.settings_muted_apps),
                    selected = prefs.mutedApps,
                ) {
                    prefs.mutedApps = it
                    render()
                }
            }
        }
    }

    /** Theme, text color, alignment, font size and grayscale. */
    private fun renderAppearance(container: LinearLayout) {
        section(container, getString(R.string.settings_section_appearance))
        row(container, getString(R.string.settings_theme), themeLabel()) { showThemePicker() }
        row(
            container,
            getString(R.string.settings_text_color),
            if (prefs.crtGreen) getString(R.string.text_green) else getString(R.string.text_white),
        ) {
            prefs.crtGreen = !prefs.crtGreen
            recreate()
        }
        row(container, getString(R.string.settings_alignment), alignmentLabel()) { showAlignmentPicker() }
        row(container, getString(R.string.settings_lock_portrait), onOff(prefs.lockPortrait)) {
            prefs.lockPortrait = !prefs.lockPortrait
            render()
        }
        row(
            container,
            getString(R.string.settings_font_size),
            if (prefs.largeFont) getString(R.string.font_large) else getString(R.string.font_normal),
        ) {
            prefs.largeFont = !prefs.largeFont
            recreate()
        }
        row(
            container,
            getString(R.string.settings_grayscale),
            onOff(systemGrayscaleEnabled()),
        ) { toggleSystemGrayscale() }
    }

    /** Hidden apps, statistics, permissions and the default-home shortcut. */
    private fun renderLauncher(container: LinearLayout) {
        section(container, getString(R.string.settings_section_launcher))
        row(container, getString(R.string.settings_hidden_apps), prefs.hiddenApps.size.toString()) {
            appLists.showMultiPicker(
                title = getString(R.string.settings_hidden_apps),
                selected = prefs.hiddenApps,
            ) {
                prefs.hiddenApps = it
                render()
            }
        }
        row(container, getString(R.string.settings_stats), "") {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        row(container, getString(R.string.settings_permissions), "") {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        if (!PermissionChecks.isDefaultHome(this)) {
            row(container, getString(R.string.settings_set_default_home), "") {
                startActivity(PermissionChecks.requestDefaultHomeIntent(this))
            }
        }
    }

    /** Settings export and import. */
    private fun renderBackup(container: LinearLayout) {
        section(container, getString(R.string.settings_section_backup))
        row(container, getString(R.string.settings_export), "") {
            exportDocument.launch(getString(R.string.backup_file_name))
        }
        row(container, getString(R.string.settings_import), "") {
            // Any type: file managers label .json files inconsistently.
            importDocument.launch(arrayOf("*/*"))
        }
    }

    /** Version of this build. */
    private fun renderAbout(container: LinearLayout) {
        section(container, getString(R.string.settings_section_about))
        row(
            container,
            getString(R.string.settings_version),
            getString(R.string.settings_version_value, BuildConfig.VERSION_NAME, BuildConfig.COMMIT),
        ) {}
        val crash = CrashLog.read(this)
        row(
            container,
            getString(R.string.settings_last_crash),
            crash?.lineSequence()?.firstOrNull()?.removePrefix("Time: ") ?: getString(R.string.crash_none),
        ) { if (crash != null) showCrash(crash) }
    }

    // ---- backup ----

    private val exportDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) exportTo(uri)
        }

    private val importDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) confirmImport(uri)
        }

    private fun exportTo(uri: Uri) {
        val written = runCatching {
            val out = contentResolver.openOutputStream(uri) ?: error("no output stream")
            out.use { it.write(prefs.exportAll().toByteArray(Charsets.UTF_8)) }
        }.isSuccess
        toast(if (written) R.string.backup_export_done else R.string.backup_export_failed)
    }

    private fun confirmImport(uri: Uri) {
        val text = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_import_title)
            .setMessage(R.string.backup_import_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (text != null && prefs.importAll(text)) {
                    App.applyNightMode(prefs.theme)
                    toast(R.string.backup_import_done)
                    recreate()
                } else {
                    toast(R.string.backup_import_failed)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** The last crash, to read, share (for example by e-mail) or delete. */
    private fun showCrash(report: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_last_crash)
            .setMessage(report)
            .setPositiveButton(R.string.crash_share) { _, _ ->
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_share_subject))
                    .putExtra(Intent.EXTRA_TEXT, report)
                startActivity(Intent.createChooser(send, getString(R.string.crash_share)))
            }
            .setNeutralButton(R.string.crash_delete) { _, _ ->
                CrashLog.clear(this)
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onOff(on: Boolean): String = getString(if (on) R.string.state_on else R.string.state_off)

    private fun toast(message: Int) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun themeLabel(): String = when (prefs.theme) {
        Prefs.THEME_LIGHT -> getString(R.string.theme_light)
        else -> getString(R.string.theme_black)
    }

    private fun showThemePicker() {
        // Legacy "dark"/"oled" values both mean Black (pure #000000)
        val values = arrayOf(Prefs.THEME_LIGHT, Prefs.THEME_OLED)
        val labels = arrayOf(getString(R.string.theme_light), getString(R.string.theme_black))
        val checked = if (prefs.theme == Prefs.THEME_LIGHT) 0 else 1
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_theme)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                prefs.theme = values[which]
                App.applyNightMode(prefs.theme)
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    private fun pickTime(currentMinute: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(
            this,
            { _, hour, minute -> onPicked(hour * Time.MINUTES_PER_HOUR + minute) },
            currentMinute / Time.MINUTES_PER_HOUR,
            currentMinute % Time.MINUTES_PER_HOUR,
            true,
        ).show()
    }

    private fun alignmentLabel(): String = when (prefs.alignment) {
        Prefs.ALIGN_CENTER -> getString(R.string.align_center)
        Prefs.ALIGN_RIGHT -> getString(R.string.align_right)
        else -> getString(R.string.align_left)
    }

    private fun showAlignmentPicker() {
        val values = arrayOf(Prefs.ALIGN_LEFT, Prefs.ALIGN_CENTER, Prefs.ALIGN_RIGHT)
        val labels = arrayOf(
            getString(R.string.align_left),
            getString(R.string.align_center),
            getString(R.string.align_right),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_alignment)
            .setSingleChoiceItems(labels, values.indexOf(prefs.alignment)) { dialog, which ->
                prefs.alignment = values[which]
                dialog.dismiss()
                render()
            }
            .show()
    }

    private fun showFocusSessionPicker() {
        val values = FOCUS_SESSION_MINUTES
        val labels = values.map { getString(R.string.settings_nudge_value, it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_focus_session)
            .setItems(labels) { _, which ->
                prefs.focusSessionUntil = System.currentTimeMillis() + values[which] * Time.MINUTE_MILLIS
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showBlockedSites() {
        val sites = prefs.blockedSites.sorted()
        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.settings_blocked_sites)
            .setNeutralButton(R.string.blocked_sites_add) { _, _ -> showAddSite() }
            .setPositiveButton(android.R.string.ok, null)
        if (sites.isEmpty()) {
            builder.setMessage(R.string.blocked_sites_empty)
        } else {
            builder.setItems(
                sites.map { getString(R.string.blocked_sites_row, it) }.toTypedArray(),
            ) { _, which ->
                StrictModeGate.loosen(this) {
                    prefs.blockedSites = prefs.blockedSites - sites[which]
                    render()
                }
            }
        }
        builder.show()
    }

    private fun showAddSite() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
            hint = getString(R.string.blocked_sites_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.blocked_sites_add)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val host = io.github.minilauncher.blocking.WebsiteMatcher.normalizeHost(input.text.toString())
                if (host == null) {
                    android.widget.Toast.makeText(
                        this,
                        R.string.blocked_sites_invalid,
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    prefs.blockedSites = prefs.blockedSites + host
                }
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showNudgePicker() {
        val values = NUDGE_INTERVAL_MINUTES
        val labels = values.map {
            if (it == 0) getString(R.string.state_off) else getString(R.string.settings_nudge_value, it)
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_nudge)
            .setSingleChoiceItems(labels, values.indexOf(prefs.nudgeIntervalMinutes)) { dialog, which ->
                prefs.nudgeIntervalMinutes = values[which]
                dialog.dismiss()
                render()
            }
            .show()
    }

    private fun showLimitsOverview() {
        val repo = AppRepository(this)
        val limits = prefs.limits.toList().sortedBy { repo.labelFor(it.first).lowercase() }
        if (limits.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.settings_limits)
                .setMessage(R.string.limits_empty_hint)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val labels = limits.map { (pkg, min) ->
            getString(R.string.limits_row, repo.labelFor(pkg), min)
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_limits)
            .setItems(labels) { _, which ->
                StrictModeGate.loosen(this) {
                    prefs.setLimit(limits[which].first, null)
                    render()
                }
            }
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // System-wide grayscale via the display daltonizer (monochromacy). Only
    // works after: adb shell pm grant io.github.minilauncher android.permission.WRITE_SECURE_SETTINGS
    private fun systemGrayscaleEnabled(): Boolean =
        Settings.Secure.getInt(contentResolver, "accessibility_display_daltonizer_enabled", 0) == 1 &&
            Settings.Secure.getInt(contentResolver, "accessibility_display_daltonizer", -1) == 0

    private fun toggleSystemGrayscale() {
        if (!PermissionChecks.hasWriteSecureSettings(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.settings_grayscale)
                .setMessage(getString(R.string.grayscale_adb_hint, packageName))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val enable = !systemGrayscaleEnabled()
        runCatching {
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", if (enable) 1 else 0)
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", 0)
        }
        render()
    }

    private fun section(container: LinearLayout, title: String) {
        val view = layoutInflater.inflate(R.layout.item_section_header, container, false) as TextView
        view.text = title
        container.addView(view)
    }

    private fun row(container: LinearLayout, title: String, value: String, onClick: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.item_setting_row, container, false)
        view.findViewById<TextView>(R.id.rowTitle).text = title
        view.findViewById<TextView>(R.id.rowValue).text = value
        view.setOnClickListener { onClick() }
        container.addView(view)
    }

    private companion object {
        /** Lengths offered for a timed focus session. */
        val FOCUS_SESSION_MINUTES = intArrayOf(15, 25, 45, 60)

        /** Reminder intervals; 0 switches the reminder off. */
        val NUDGE_INTERVAL_MINUTES = intArrayOf(0, 5, 10, 15, 30, 60)
    }
}
