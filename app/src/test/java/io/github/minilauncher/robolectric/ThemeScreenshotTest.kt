package io.github.minilauncher.robolectric

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.minilauncher.App
import io.github.minilauncher.R
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.model.BlockReason
import io.github.minilauncher.data.model.BlockedInfo
import io.github.minilauncher.ui.blocked.BlockedActivity
import io.github.minilauncher.ui.drawer.AppDrawerActivity
import io.github.minilauncher.ui.home.HomeActivity
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the home screen, drawer and block screen in every theme and
 * text color, compared against the reference images in src/test/screenshots.
 * Catches unreadable combinations such as green text on white (audit B06).
 * New references: run the "Record screenshots" workflow.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h720dp-xhdpi")
class ThemeScreenshotTest {

    @get:Rule
    val screens = OpenScreens()

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun blackWhite() = captureAll(theme = Prefs.THEME_OLED, green = false)

    @Test
    fun blackGreen() = captureAll(theme = Prefs.THEME_OLED, green = true)

    @Test
    fun lightBlack() = captureAll(theme = Prefs.THEME_LIGHT, green = false)

    @Test
    fun lightGreen() = captureAll(theme = Prefs.THEME_LIGHT, green = true)

    private fun captureAll(theme: String, green: Boolean) {
        Prefs.resetForTests()
        AppRepository.invalidate()
        val prefs = Prefs.get(context)
        prefs.theme = theme
        prefs.crtGreen = green
        prefs.onboardingDone = true
        listOf("Calendar", "Camera", "Clock", "Contacts", "Maps", "Messages", "Music", "Notes", "Phone", "Photos")
            .forEach { installLauncherApp(context, "com.example.${it.lowercase()}", it) }
        prefs.favorites = listOf("com.example.phone", "com.example.messages", "com.example.maps")
        App.applyNightMode(theme)
        val name = "${if (theme == Prefs.THEME_LIGHT) "light" else "black"}-${if (green) "green" else "plain"}"

        val home = screens.start(HomeActivity::class.java)
        waitUntil { listSize(home, R.id.favoritesList) == 3 }
        // The clock and date change every minute; leave them out of the picture.
        home.findViewById<View>(R.id.clock).visibility = View.INVISIBLE
        home.findViewById<View>(R.id.date).visibility = View.INVISIBLE
        capture(home, "home-$name")

        val drawer = screens.start(AppDrawerActivity::class.java)
        waitUntil { listSize(drawer, R.id.appList) == 10 }
        capture(drawer, "drawer-$name")

        val blockIntent = BlockedActivity.intent(
            context,
            BlockedInfo("com.example.photos", BlockReason.SCHEDULE, "17:00"),
            "Photos",
        )
        val blocked = screens.start(BlockedActivity::class.java, blockIntent)
        blocked.findViewById<View>(R.id.blockedUsage).visibility = View.INVISIBLE
        capture(blocked, "blocked-$name")
    }

    private fun listSize(activity: Activity, id: Int) =
        activity.findViewById<RecyclerView>(id).adapter?.itemCount ?: 0

    private fun capture(activity: Activity, name: String) {
        // A layout pass can post work that changes the layout again (the
        // drawer's room around its rows); let a few frames run first.
        repeat(SETTLE_FRAMES) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MILLIS)) }
        activity.window.decorView.captureRoboImage("src/test/screenshots/$name.png")
    }

    private companion object {
        const val SETTLE_FRAMES = 3
        const val FRAME_MILLIS = 50L
    }
}
