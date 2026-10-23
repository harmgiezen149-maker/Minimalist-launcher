package io.github.minilauncher.ui.common

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.github.minilauncher.R
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.model.AppEntry
import io.github.minilauncher.data.model.AppVisibility
import io.github.minilauncher.util.InputRules

/** One menu line: its label and what tapping it does. */
private typealias Option = Pair<String, () -> Unit>

/** Long-press menu on any app row: favorite, rename, hide, block, limit, info. */
object AppLongPressDialog {

    fun show(activity: Activity, entry: AppEntry, onChanged: () -> Unit) {
        val options = launcherOptions(activity, entry, onChanged) +
            focusOptions(activity, entry, onChanged) +
            modeOptions(activity, entry, onChanged) +
            folderOption(activity, entry.packageName, onChanged) +
            appInfoOption(activity, entry.packageName)

        AlertDialog.Builder(activity)
            .setTitle(entry.displayLabel)
            .setItems(options.map { it.first }.toTypedArray()) { _, which ->
                options[which].second()
                onChanged()
            }
            .show()
    }

    /** Favorite, rename and hide: how the app appears in the launcher. */
    private fun launcherOptions(activity: Activity, entry: AppEntry, onChanged: () -> Unit): List<Option> {
        val prefs = Prefs.get(activity)
        val pkg = entry.packageName
        return listOf(
            if (entry.isFavorite) {
                activity.getString(R.string.action_remove_favorite) to { prefs.removeFavorite(pkg) }
            } else {
                activity.getString(R.string.action_add_favorite) to { prefs.addFavorite(pkg) }
            },
            activity.getString(R.string.action_rename) to { showRename(activity, entry, onChanged) },
            if (entry.isHidden) {
                activity.getString(R.string.action_unhide) to { prefs.hiddenApps = prefs.hiddenApps - pkg }
            } else {
                activity.getString(R.string.action_hide) to { prefs.hiddenApps = prefs.hiddenApps + pkg }
            },
        )
    }

    /** Block, mindful pause, daily limit, mute and reminders. */
    private fun focusOptions(activity: Activity, entry: AppEntry, onChanged: () -> Unit): List<Option> {
        val prefs = Prefs.get(activity)
        val pkg = entry.packageName
        val limit = prefs.limits[pkg]
        return listOf(
            if (pkg in prefs.blockedApps) {
                activity.getString(R.string.action_unblock) to {
                    StrictModeGate.loosen(activity) { prefs.blockedApps = prefs.blockedApps - pkg }
                }
            } else {
                activity.getString(R.string.action_block) to { prefs.blockedApps = prefs.blockedApps + pkg }
            },
            if (pkg in prefs.mindfulApps) {
                activity.getString(R.string.action_mindful_off) to {
                    StrictModeGate.loosen(activity) { prefs.mindfulApps = prefs.mindfulApps - pkg }
                }
            } else {
                activity.getString(R.string.action_mindful_on) to { prefs.mindfulApps = prefs.mindfulApps + pkg }
            },
            (
                limit?.let { activity.getString(R.string.action_edit_limit, it) }
                    ?: activity.getString(R.string.action_set_limit)
                )
                to { showLimit(activity, entry, onChanged) },
            if (pkg in prefs.mutedApps) {
                activity.getString(R.string.action_unmute) to { prefs.mutedApps = prefs.mutedApps - pkg }
            } else {
                activity.getString(R.string.action_mute) to { prefs.mutedApps = prefs.mutedApps + pkg }
            },
            if (pkg in prefs.nudgeExemptApps) {
                activity.getString(R.string.action_nudge_include) to {
                    prefs.nudgeExemptApps = prefs.nudgeExemptApps - pkg
                }
            } else {
                activity.getString(R.string.action_nudge_exempt) to {
                    prefs.nudgeExemptApps = prefs.nudgeExemptApps + pkg
                }
            },
        )
    }

    /** Day/evening visibility and notification window; only while that mode is on. */
    private fun modeOptions(activity: Activity, entry: AppEntry, onChanged: () -> Unit): List<Option> {
        val prefs = Prefs.get(activity)
        if (!prefs.dayEveningEnabled) return emptyList()
        val pkg = entry.packageName
        return listOf(
            activity.getString(R.string.action_visibility, visibilityLabel(activity, prefs.visibilityFor(pkg)))
                to { showVisibilityPicker(activity, entry, onChanged) },
            activity.getString(
                R.string.action_notification_window,
                notificationWindowLabel(activity, prefs.notificationWindowFor(pkg)),
            ) to { showNotificationWindowPicker(activity, entry, onChanged) },
        )
    }

    private fun folderOption(activity: Activity, pkg: String, onChanged: () -> Unit): Option {
        val prefs = Prefs.get(activity)
        val folder = prefs.folderContaining(pkg)
        return if (folder != null) {
            activity.getString(R.string.action_remove_from_folder, folder.name) to { prefs.removeFromFolder(pkg) }
        } else {
            activity.getString(R.string.action_add_to_folder) to { showFolderPicker(activity, pkg, onChanged) }
        }
    }

    private fun appInfoOption(activity: Activity, pkg: String): Option =
        activity.getString(R.string.action_app_info) to {
            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
        }

    fun visibilityLabel(activity: Activity, visibility: AppVisibility): String =
        activity.getString(
            when (visibility) {
                AppVisibility.ALWAYS -> R.string.visibility_always
                AppVisibility.DAY -> R.string.visibility_day
                AppVisibility.EVENING -> R.string.visibility_evening
            },
        )

    /** Lets the user pick when an app shows up: always, day only or evening only. */
    fun showVisibilityPicker(activity: Activity, entry: AppEntry, onChanged: () -> Unit) {
        val prefs = Prefs.get(activity)
        val values = AppVisibility.entries.toTypedArray()
        val labels = values.map { visibilityLabel(activity, it) }.toTypedArray()
        val current = prefs.visibilityFor(entry.packageName)
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.visibility_title, entry.displayLabel))
            .setSingleChoiceItems(labels, values.indexOf(current)) { dialog, which ->
                prefs.setVisibility(entry.packageName, values[which])
                dialog.dismiss()
                onChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun notificationWindowLabel(activity: Activity, window: AppVisibility): String =
        activity.getString(
            when (window) {
                AppVisibility.ALWAYS -> R.string.notif_window_always
                AppVisibility.DAY -> R.string.notif_window_day
                AppVisibility.EVENING -> R.string.notif_window_evening
            },
        )

    /** Lets the user pick when an app may notify: always, day only or evening only. */
    fun showNotificationWindowPicker(activity: Activity, entry: AppEntry, onChanged: () -> Unit) {
        val prefs = Prefs.get(activity)
        val values = AppVisibility.entries.toTypedArray()
        val labels = values.map { notificationWindowLabel(activity, it) }.toTypedArray()
        val current = prefs.notificationWindowFor(entry.packageName)
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.notification_window_title, entry.displayLabel))
            .setSingleChoiceItems(labels, values.indexOf(current)) { dialog, which ->
                prefs.setNotificationWindow(entry.packageName, values[which])
                dialog.dismiss()
                onChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFolderPicker(activity: Activity, pkg: String, onChanged: () -> Unit) {
        val prefs = Prefs.get(activity)
        val folders = prefs.folders
        val labels = folders.map { it.name }.toTypedArray() +
            activity.getString(R.string.folder_new)
        AlertDialog.Builder(activity)
            .setTitle(R.string.action_add_to_folder)
            .setItems(labels) { _, which ->
                if (which < folders.size) {
                    prefs.addToFolder(folders[which].id, pkg)
                    onChanged()
                } else {
                    showNewFolder(activity, pkg, onChanged)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showNewFolder(activity: Activity, pkg: String, onChanged: () -> Unit) {
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = activity.getString(R.string.folder_name_hint)
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.folder_new)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) Prefs.get(activity).createFolder(name, pkg)
                onChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showRename(activity: Activity, entry: AppEntry, onChanged: () -> Unit) {
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(entry.displayLabel)
            setSelection(text.length)
        }
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.rename_title, entry.originalLabel))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                Prefs.get(activity).setRename(
                    entry.packageName,
                    if (name.isEmpty() || name == entry.originalLabel) null else name,
                )
                onChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLimit(activity: Activity, entry: AppEntry, onChanged: () -> Unit) {
        val prefs = Prefs.get(activity)
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = activity.getString(R.string.limit_hint)
            prefs.limits[entry.packageName]?.let { setText(it.toString()) }
        }
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.limit_title, entry.displayLabel))
            .setMessage(R.string.limit_message)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val minutes = InputRules.parseLimitMinutes(input.text.toString())
                if (minutes == null) {
                    // Nothing is saved: an empty or impossible value used to
                    // remove the limit without saying so.
                    Toast.makeText(activity, R.string.limit_invalid, Toast.LENGTH_LONG).show()
                } else {
                    val current = prefs.limits[entry.packageName]
                    val save = { prefs.setLimit(entry.packageName, minutes) }
                    // A higher limit loosens; a new or lower one only tightens.
                    if (current != null && minutes > current) StrictModeGate.loosen(activity, save) else save()
                }
                onChanged()
            }
            .setNeutralButton(R.string.limit_remove) { _, _ ->
                StrictModeGate.loosen(activity) { prefs.setLimit(entry.packageName, null) }
                onChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
