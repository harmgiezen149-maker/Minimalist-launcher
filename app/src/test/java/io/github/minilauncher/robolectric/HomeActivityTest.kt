package io.github.minilauncher.robolectric

import android.app.Application
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
