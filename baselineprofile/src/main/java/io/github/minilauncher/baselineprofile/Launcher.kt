package io.github.minilauncher.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val TARGET = "io.github.minilauncher"
private const val WAIT_MILLIS = 5_000L

/**
 * The launcher has no app-icon entry, only the home-screen role, so it is
 * started by its activity name.
 */
internal fun MacrobenchmarkScope.startHomeScreen() {
    startActivityAndWait(Intent().setClassName(TARGET, "$TARGET.ui.home.HomeActivity"))
}

/** First start opens the permission checklist on top; leave it, then open the drawer. */
internal fun MacrobenchmarkScope.openDrawer() {
    if (device.wait(Until.hasObject(By.res(TARGET, "stepsContainer")), WAIT_MILLIS)) device.pressBack()
    device.wait(Until.findObject(By.res(TARGET, "allAppsButton")), WAIT_MILLIS)?.click()
    device.wait(Until.hasObject(By.res(TARGET, "appList")), WAIT_MILLIS)
    device.pressBack()
}
