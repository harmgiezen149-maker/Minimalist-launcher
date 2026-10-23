package io.github.minilauncher.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.github.minilauncher.R
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.ui.common.AppLongPressDialog
import io.github.minilauncher.ui.common.PermissionChecks

/**
 * The settings screen's app-list dialogs: pick several apps, or walk through
 * every app to set its day/evening visibility or notification window.
 * [onChanged] lets the settings screen refresh its counts.
 */
class AppListDialogs(private val activity: Activity, private val onChanged: () -> Unit) {

    private val prefs = Prefs.get(activity)

    /**
     * Every app with its day/evening visibility; tap one to change it. After a
     * change the list reopens scrolled to [focusPackage], so several apps can
     * be set in a row without hunting through the list again.
     */
    fun showVisibilityList(focusPackage: String? = null) {
        val apps = AppRepository(activity).allApps(includeHidden = true, respectMode = false)
        val labels = apps.map {
            activity.getString(
                R.string.app_visibility_row,
                it.displayLabel,
                AppLongPressDialog.visibilityLabel(activity, prefs.visibilityFor(it.packageName)),
            )
        }.toTypedArray()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.settings_app_visibility)
            .setItems(labels) { _, which ->
                val pkg = apps[which].packageName
                AppLongPressDialog.showVisibilityPicker(activity, apps[which]) {
                    onChanged()
                    showVisibilityList(focusPackage = pkg)
                }
            }
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.show()
        scrollToApp(dialog, apps.indexOfFirst { it.packageName == focusPackage })
    }

    /** Keeps the just-configured app in view when a picker list reopens. */
    private fun scrollToApp(dialog: AlertDialog, index: Int) {
        if (index < 0) return
        val list = dialog.listView ?: return
        list.post {
            // Roughly a third down, so it reads as "still where you were".
            list.setSelectionFromTop(index, list.height / REOPEN_OFFSET_DIVISOR)
        }
    }

    /** Every app with its notification window; tap one to change it. */
    fun showNotificationWindowList(focusPackage: String? = null) {
        // Only warn when opening the list, not on every reopen after a change.
        if (focusPackage == null && !PermissionChecks.isNotificationListenerEnabled(activity)) {
            Toast.makeText(
                activity,
                R.string.notification_windows_needs_access,
                Toast.LENGTH_LONG,
            ).show()
        }
        val apps = AppRepository(activity).allApps(includeHidden = true, respectMode = false)
        val labels = apps.map {
            activity.getString(
                R.string.app_visibility_row,
                it.displayLabel,
                AppLongPressDialog.notificationWindowLabel(
                    activity,
                    prefs.notificationWindowFor(it.packageName),
                ),
            )
        }.toTypedArray()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.settings_notification_windows)
            .setItems(labels) { _, which ->
                val pkg = apps[which].packageName
                AppLongPressDialog.showNotificationWindowPicker(activity, apps[which]) {
                    onChanged()
                    showNotificationWindowList(focusPackage = pkg)
                }
            }
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.show()
        scrollToApp(dialog, apps.indexOfFirst { it.packageName == focusPackage })
    }

    /** Every app with a checkbox; [onDone] gets the checked packages. */
    fun showMultiPicker(
        title: String,
        selected: Set<String>,
        onDone: (Set<String>) -> Unit,
    ) {
        val apps = AppRepository(activity).allApps(includeHidden = true, respectMode = false)
        val labels = apps.map { it.displayLabel }.toTypedArray()
        val checked = apps.map { it.packageName in selected }.toBooleanArray()
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onDone(
                    apps.filterIndexed { index, _ -> checked[index] }
                        .map { it.packageName }
                        .toSet(),
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private companion object {
        /** A reopened picker shows the last-changed app a third of the way down. */
        const val REOPEN_OFFSET_DIVISOR = 3
    }
}
