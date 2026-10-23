package io.github.minilauncher.robolectric

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.minilauncher.data.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrefsTest {

    private lateinit var prefs: Prefs

    @Before
    fun setUp() {
        Prefs.resetForTests()
        prefs = Prefs.get(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `export and import bring every setting back`() {
        prefs.favorites = listOf("com.a", "com.b")
        prefs.blockedApps = setOf("com.x")
        prefs.setLimit("com.x", 30)
        prefs.crtGreen = true
        prefs.nudgeIntervalMinutes = 10
        prefs.focusSessionUntil = 1_700_000_000_000L
        val folderId = prefs.createFolder("Games", "com.game")
        val backup = prefs.exportAll()

        prefs.favorites = emptyList()
        prefs.blockedApps = emptySet()
        prefs.setLimit("com.x", null)
        prefs.crtGreen = false
        prefs.nudgeIntervalMinutes = 0
        prefs.focusSessionUntil = 0L
        prefs.deleteFolder(folderId)

        assertTrue(prefs.importAll(backup))
        assertEquals(listOf("com.a", "com.b"), prefs.favorites)
        assertEquals(setOf("com.x"), prefs.blockedApps)
        assertEquals(mapOf("com.x" to 30), prefs.limits)
        assertTrue(prefs.crtGreen)
        assertEquals(10, prefs.nudgeIntervalMinutes)
        assertEquals(1_700_000_000_000L, prefs.focusSessionUntil)
        assertEquals("Games", prefs.folderContaining("com.game")?.name)
    }

    @Test
    fun `import replaces settings that are not in the file`() {
        val backup = prefs.exportAll()
        prefs.blockedApps = setOf("com.added.later")
        assertTrue(prefs.importAll(backup))
        assertEquals(emptySet<String>(), prefs.blockedApps)
    }

    @Test
    fun `import keeps this phone's onboarding progress`() {
        prefs.onboardingDone = true
        val backup = prefs.exportAll()
        prefs.onboardingDone = false
        assertTrue(prefs.importAll(backup))
        assertFalse(prefs.onboardingDone)
    }

    @Test
    fun `an invalid file changes nothing`() {
        prefs.favorites = listOf("com.a")
        assertFalse(prefs.importAll("""{"hello": "world"}"""))
        assertFalse(prefs.importAll("not json"))
        assertEquals(listOf("com.a"), prefs.favorites)
    }

    @Test
    fun `an app lives in at most one folder`() {
        prefs.createFolder("A", "com.app")
        val second = prefs.createFolder("B")
        prefs.addToFolder(second, "com.app")
        assertEquals(second, prefs.folderContaining("com.app")?.id)
        assertEquals(1, prefs.folders.count { "com.app" in it.packages })
    }

    @Test
    fun `a limit of zero or less removes it`() {
        prefs.setLimit("com.x", 30)
        prefs.setLimit("com.x", 0)
        assertEquals(emptyMap<String, Int>(), prefs.limits)
    }

    @Test
    fun `expired allowances are dropped when a new one is written`() {
        prefs.allowTemporarily("old", System.currentTimeMillis() - 1)
        prefs.allowTemporarily("new", System.currentTimeMillis() + 60_000L)
        assertEquals(setOf("new"), prefs.tempAllowUntil.keys)
    }

    @Test
    fun `strict mode and portrait lock have safe defaults`() {
        assertFalse(prefs.strictMode)
        assertTrue(prefs.lockPortrait)
    }
}
