package io.github.minilauncher.robolectric

import android.app.Application
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.minilauncher.App
import io.github.minilauncher.R
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.ui.home.HomeActivity
import io.github.minilauncher.ui.onboarding.OnboardingActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class HomeActivityTest {

    @get:Rule
    val screens = OpenScreens()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: Prefs

    @Before
    fun setUp() {
        Prefs.resetForTests()
        AppRepository.invalidate()
        prefs = Prefs.get(context)
        prefs.onboardingDone = true
        installLauncherApp(context, "com.mail", "Mail")
        installLauncherApp(context, "com.maps", "Maps")
    }

    private fun favoritesCount(home: HomeActivity) =
        home.findViewById<RecyclerView>(R.id.favoritesList).adapter?.itemCount ?: 0

    @Test
    fun `without favorites the home screen explains how to add them`() {
        val home = screens.start(HomeActivity::class.java)
        waitUntil { home.findViewById<View>(R.id.emptyHint).visibility == View.VISIBLE }
        assertEquals(0, favoritesCount(home))
    }

    @Test
    fun `favorites are listed in the chosen order`() {
        prefs.favorites = listOf("com.maps", "com.mail")
        val home = screens.start(HomeActivity::class.java)
        waitUntil { favoritesCount(home) == 2 }
        assertEquals(View.GONE, home.findViewById<View>(R.id.emptyHint).visibility)
    }

    @Test
    fun `the first start opens the permission checklist`() {
        prefs.onboardingDone = false
        val home = screens.start(HomeActivity::class.java)
        assertEquals(
            OnboardingActivity::class.java.name,
            shadowOf(home).nextStartedActivity.component?.className,
        )
    }

    @Test
    fun `back on the home screen does not close it`() {
        val home = screens.start(HomeActivity::class.java)
        home.onBackPressedDispatcher.onBackPressed()
        assertEquals(false, home.isFinishing)
    }

    @Test
    fun `green text is green on the black theme too`() {
        prefs.theme = Prefs.THEME_OLED
        prefs.crtGreen = true
        App.applyNightMode(Prefs.THEME_OLED)
        val home = screens.start(HomeActivity::class.java)
        assertEquals(
            home.getColor(R.color.crt_green),
            home.findViewById<TextView>(R.id.allAppsButton).currentTextColor,
        )
    }

    @Test
    fun `a shortcut to an installed app shows its name`() {
        prefs.shortcutRight = "com.mail"
        val home = screens.start(HomeActivity::class.java)
        val shortcut = home.findViewById<TextView>(R.id.shortcutRight)
        waitUntil { shortcut.text.toString() == "Mail" }
    }

    @Test
    fun `a shortcut to an app that is not installed offers a choice`() {
        prefs.shortcutRight = "com.not.installed"
        val home = screens.start(HomeActivity::class.java)
        val shortcut = home.findViewById<TextView>(R.id.shortcutRight)
        waitUntil { shortcut.text.toString() == home.getString(R.string.shortcut_choose) }
        assertEquals(home.getString(R.string.shortcut_choose_description), shortcut.contentDescription)
    }
}
