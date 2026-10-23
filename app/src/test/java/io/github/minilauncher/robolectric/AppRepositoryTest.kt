package io.github.minilauncher.robolectric

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AppRepositoryTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository

    @Before
    fun setUp() {
        Prefs.resetForTests()
        AppRepository.invalidate()
        prefs = Prefs.get(context)
        installLauncherApp(context, "com.zeta", "Zeta")
        installLauncherApp(context, "com.alpha", "alpha")
        installLauncherApp(context, "com.mid", "Mid")
        repo = AppRepository(context)
    }

    @Test
    fun `lists launchable apps sorted by name, ignoring case`() {
        assertEquals(listOf("alpha", "Mid", "Zeta"), repo.allApps().map { it.displayLabel })
    }

    @Test
    fun `hidden apps only show when asked for`() {
        prefs.hiddenApps = setOf("com.mid")
        assertEquals(listOf("com.alpha", "com.zeta"), repo.allApps().map { it.packageName })
        assertEquals(3, repo.allApps(includeHidden = true).size)
    }

    @Test
    fun `renamed apps use the new name and sort by it`() {
        prefs.setRename("com.zeta", "Aardvark")
        assertEquals(listOf("Aardvark", "alpha", "Mid"), repo.allApps().map { it.displayLabel })
        assertEquals("Aardvark", repo.labelFor("com.zeta"))
    }

    @Test
    fun `favorites keep the user's order and skip removed apps`() {
        prefs.favorites = listOf("com.zeta", "com.gone", "com.alpha")
        assertEquals(listOf("com.zeta", "com.alpha"), repo.favoriteApps().map { it.packageName })
    }

    @Test
    fun `launching an app starts its main activity`() {
        assertTrue(repo.launch("com.alpha"))
        assertEquals("com.alpha", shadowOf(context).nextStartedActivity.component?.packageName)
    }

    @Test
    fun `launching an app that is not installed fails without crashing`() {
        assertFalse(repo.launch("com.not.installed"))
    }
}
