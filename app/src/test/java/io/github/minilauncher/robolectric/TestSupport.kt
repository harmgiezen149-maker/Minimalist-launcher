package io.github.minilauncher.robolectric

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Looper
import org.robolectric.Shadows.shadowOf

/** Installs a fake app with a launcher entry, as the drawer and home screen list them. */
fun installLauncherApp(context: Context, packageName: String, label: String) {
    val packageManager = shadowOf(context.packageManager)
    packageManager.installPackage(
        PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                nonLocalizedLabel = label
            }
        },
    )
    val component = ComponentName(packageName, "$packageName.Main")
    packageManager.addActivityIfNotPresent(component)
    packageManager.addIntentFilterForActivity(
        component,
        IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
    )
}

/**
 * The screens load app lists on a background thread and post the result to
 * the main thread. This runs the main thread's queue until [condition] holds.
 */
fun waitUntil(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (true) {
        shadowOf(Looper.getMainLooper()).idle()
        if (condition()) return
        check(System.currentTimeMillis() < deadline) { "Condition not met within $timeoutMillis ms" }
        Thread.sleep(POLL_MILLIS)
    }
}

private const val POLL_MILLIS = 10L
