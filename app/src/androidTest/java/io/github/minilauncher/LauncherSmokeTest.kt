package io.github.minilauncher

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.ui.home.HomeActivity
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests on a real (emulated) device: the paths a person uses every day.
 * Espresso's accessibility checks run on every interaction, so too-small
 * touch targets or unlabeled controls fail the test too.
 */
@RunWith(AndroidJUnit4::class)
class LauncherSmokeTest {

    @Before
    fun setUp() {
        Prefs.get(ApplicationProvider.getApplicationContext()).onboardingDone = true
    }

    @Test
    fun homeScreenShowsItsButtons() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.allAppsButton)).check(matches(isDisplayed()))
            onView(withId(R.id.settingsButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun drawerSearchShowsResultsWhileTheKeyboardIsOpen() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.allAppsButton)).perform(click())
            // Every emulator image has the system Settings app.
            onView(withId(R.id.searchField)).perform(typeText("Settings"))
            onView(allOf(withText("Settings"), withParent(withId(R.id.appList)))).check(matches(isDisplayed()))
        }
    }

    @Test
    fun settingsOpenFromTheHomeScreen() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.settingsButton)).perform(click())
            onView(withText(R.string.settings_section_focus)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.allAppsButton)).check(matches(isDisplayed()))
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun enableAccessibilityChecks() {
            AccessibilityChecks.enable().setRunChecksFromRootView(true)
        }
    }
}
