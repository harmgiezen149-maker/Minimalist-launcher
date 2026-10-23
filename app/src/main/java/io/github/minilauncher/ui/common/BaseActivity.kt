package io.github.minilauncher.ui.common

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.minilauncher.R
import io.github.minilauncher.data.Prefs

/**
 * Applies the chosen font scale and CRT text color to every screen. Both are
 * baked in when a screen is created, so a screen that was already open (the
 * home screen, always) rebuilds itself when it returns and finds them changed.
 */
abstract class BaseActivity : AppCompatActivity() {

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
    }

    override fun onResume() {
        super.onResume()
        if (builtWith != currentAppearance()) recreate()
    }

    private companion object {
        const val LARGE_FONT_SCALE = 1.2f
    }
}
