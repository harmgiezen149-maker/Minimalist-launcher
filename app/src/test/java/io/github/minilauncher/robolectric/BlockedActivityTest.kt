package io.github.minilauncher.robolectric

import android.app.Application
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.minilauncher.R
import io.github.minilauncher.blocking.BlockState
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.model.BlockReason
import io.github.minilauncher.data.model.BlockedInfo
import io.github.minilauncher.ui.blocked.BlockedActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class BlockedActivityTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: Prefs

    @Before
    fun setUp() {
        Prefs.resetForTests()
        prefs = Prefs.get(context)
    }

    private fun show(reason: BlockReason, detail: String = ""): BlockedActivity {
        val intent = BlockedActivity.intent(context, BlockedInfo("com.app", reason, detail), "Some App")
        return Robolectric.buildActivity(BlockedActivity::class.java, intent).setup().get()
    }

    private fun BlockedActivity.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun BlockedActivity.fiveMoreVisibility() = findViewById<View>(R.id.fiveMoreButton).visibility

    @Test
    fun `a reached limit says so and offers no five more minutes`() {
        val screen = show(BlockReason.LIMIT, "30")
        assertEquals("Some App", screen.text(R.id.blockedAppName))
        assertEquals(context.getString(R.string.blocked_reason_limit, "30"), screen.text(R.id.blockedReason))
        assertEquals(View.GONE, screen.fiveMoreVisibility())
    }

    @Test
    fun `focus mode offers five more minutes while that is allowed`() {
        assertEquals(View.VISIBLE, show(BlockReason.FOCUS_MODE).fiveMoreVisibility())
    }

    @Test
    fun `five more minutes can be switched off`() {
        prefs.allowFiveMoreMinutes = false
        assertEquals(View.GONE, show(BlockReason.FOCUS_MODE).fiveMoreVisibility())
    }

    @Test
    fun `strict mode withholds five more minutes during a focus session`() {
        prefs.strictMode = true
        prefs.focusSessionUntil = System.currentTimeMillis() + 10 * 60_000L
        assertEquals(View.GONE, show(BlockReason.FOCUS_MODE).fiveMoreVisibility())
    }

    @Test
    fun `a schedule block names the end of the window`() {
        val screen = show(BlockReason.SCHEDULE, "17:00")
        assertEquals(context.getString(R.string.blocked_reason_schedule, "17:00"), screen.text(R.id.blockedReason))
    }

    @Test
    fun `a website block names the site instead of the browser`() {
        assertEquals("youtube.com", show(BlockReason.WEBSITE, "youtube.com").text(R.id.blockedAppName))
    }

    @Test
    fun `showing the screen clears the home screen's fallback`() {
        BlockState.post(BlockedInfo("com.app", BlockReason.FOCUS_MODE, ""))
        show(BlockReason.FOCUS_MODE)
        assertNull(BlockState.pendingBlock)
    }
}
