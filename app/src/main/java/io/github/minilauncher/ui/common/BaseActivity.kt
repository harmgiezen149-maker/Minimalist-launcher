package io.github.minilauncher.ui.common

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.minilauncher.R
import io.github.minilauncher.data.Prefs

/**
 * Applies the chosen font scale and CRT text color to every screen. Both are
 * baked in when a screen is created, so a screen that was already open (the
 * home screen, always) rebuilds itself when it returns and finds them changed.
 *
 * Every screen draws edge to edge, as Android 15+ requires; the system bars,
 * display cutout and keyboard become padding on the screen's root view.
 */
abstract class BaseActivity : AppCompatActivity() {

    /** Home and drawer follow the portrait-lock setting; other screens rotate freely. */
    protected open val followsPortraitLock = false

    /** The appearance this screen was built with. */
    private var builtWith: Appearance? = null

    private data class Appearance(val crtGreen: Boolean, val largeFont: Boolean)

    private fun currentAppearance(): Appearance = Prefs.get(this).let { Appearance(it.crtGreen, it.largeFont) }

    override fun attachBaseContext(newBase: Context) {
        val prefs = Prefs.get(newBase)
        if (prefs.largeFont) {
            val config = Configuration(newBase.resources.configuration)
            config.fontScale = config.fontScale * LARGE_FONT_SCALE
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearance = currentAppearance()
        builtWith = appearance
        if (appearance.crtGreen) {
            theme.applyStyle(R.style.ThemeOverlay_MiniLauncher_Crt, true)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0)?.let(::padForSystemBars)
    }

    override fun onResume() {
        super.onResume()
        if (builtWith != currentAppearance()) recreate()
        if (followsPortraitLock) {
            requestedOrientation = if (Prefs.get(this).lockPortrait) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    /** Adds the bars, cutout and keyboard to [root]'s own padding from the layout. */
    private fun padForSystemBars(root: View) {
        val own = Insets.of(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
        val types = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val bars = windowInsets.getInsets(types)
            view.setPadding(own.left + bars.left, own.top + bars.top, own.right + bars.right, own.bottom + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private companion object {
        const val LARGE_FONT_SCALE = 1.2f
    }
}
