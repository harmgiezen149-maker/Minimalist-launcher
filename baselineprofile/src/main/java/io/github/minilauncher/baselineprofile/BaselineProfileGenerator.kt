package io.github.minilauncher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which code runs while the home screen starts and the drawer opens,
 * so Android compiles exactly that ahead of time and the launcher appears
 * faster after every update.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun homeScreenAndDrawer() = rule.collect(packageName = TARGET, includeInStartupProfile = true) {
        pressHome()
        startHomeScreen()
        openDrawer()
    }
}
