package io.github.minilauncher.robolectric

import android.app.Application
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.minilauncher.R
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.ui.drawer.AppDrawerActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDrawerActivityTest {

    @get:Rule
    val screens = OpenScreens()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: Prefs

    @Before
    fun setUp() {
        Prefs.resetForTests()
        AppRepository.invalidate()
        prefs = Prefs.get(context)
        installLauncherApp(context, "com.chess", "Chess")
        installLauncherApp(context, "com.calendar", "Calendar")
        installLauncherApp(context, "com.notes", "Notes")
    }

    private fun rowCount(drawer: AppDrawerActivity) =
        drawer.findViewById<RecyclerView>(R.id.appList).adapter?.itemCount ?: 0

    private fun open(): AppDrawerActivity {
        val drawer = screens.start(AppDrawerActivity::class.java)
        waitUntil { rowCount(drawer) > 0 }
        return drawer
    }

    @Test
    fun `lists every app`() {
        assertEquals(3, rowCount(open()))
    }

    @Test
    fun `search narrows the list, ignoring case`() {
        val drawer = open()
        drawer.findViewById<EditText>(R.id.searchField).setText("CH")
        assertEquals(1, rowCount(drawer))
    }

    @Test
    fun `apps in a folder sit behind the folder row, but search still finds them`() {
        prefs.createFolder("Games", "com.chess")
        val drawer = open()
        assertEquals(3, rowCount(drawer)) // the folder, Calendar and Notes
        drawer.findViewById<EditText>(R.id.searchField).setText("chess")
        assertEquals(1, rowCount(drawer))
    }

    @Test
    fun `back closes a folder before it closes the drawer`() {
        prefs.createFolder("Games", "com.chess")
        val drawer = open()
        val folderRow = drawer.findViewById<RecyclerView>(R.id.appList).findViewHolderForAdapterPosition(0)
        requireNotNull(folderRow) { "the folder row was not laid out" }.itemView.performClick()
        assertEquals(2, rowCount(drawer)) // "back" row and Chess
        drawer.onBackPressedDispatcher.onBackPressed()
        assertFalse(drawer.isFinishing)
        assertEquals(3, rowCount(drawer))
        drawer.onBackPressedDispatcher.onBackPressed()
        assertTrue(drawer.isFinishing)
    }
}
