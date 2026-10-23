package io.github.minilauncher.ui.pause

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import io.github.minilauncher.R
import io.github.minilauncher.blocking.BlockState
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.ui.common.AppLauncher
import io.github.minilauncher.ui.common.BaseActivity
import io.github.minilauncher.util.Time

/**
 * The mindful pause: a short breathing animation, then "how long do you want
 * to use this app?" — the chosen duration becomes a temporary allowance.
 */
class PauseActivity : BaseActivity() {

    private var animator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pause)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }
        BlockState.clearFor(pkg)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: pkg
        val prefs = Prefs.get(this)

        findViewById<TextView>(R.id.pauseAppName).text = label

        val durationsRow = findViewById<LinearLayout>(R.id.durationsRow)
        val durationsTitle = findViewById<TextView>(R.id.durationsTitle)
        for (minutes in DURATION_MINUTES) {
            val button = layoutInflater.inflate(R.layout.item_app_text, durationsRow, false) as TextView
            button.text = getString(R.string.pause_minutes, minutes)
            button.setOnClickListener {
                prefs.allowTemporarily(pkg, System.currentTimeMillis() + minutes * Time.MINUTE_MILLIS)
                AppLauncher.open(this, pkg)
                finish()
            }
            durationsRow.addView(button)
        }

        findViewById<TextView>(R.id.neverMindButton).setOnClickListener { finish() }

        val circle = findViewById<View>(R.id.breathingCircle)
        animator = ValueAnimator.ofFloat(BREATH_MIN_SCALE, 1f).apply {
            duration = BREATH_MILLIS
            interpolator = LinearInterpolator()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = BREATH_REPEATS
            addUpdateListener { anim ->
                val scale = anim.animatedValue as Float
                circle.scaleX = scale
                circle.scaleY = scale
                circle.alpha = BREATH_MIN_ALPHA + (1f - BREATH_MIN_ALPHA) * scale
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isFinishing || isDestroyed) return
                    durationsTitle.visibility = View.VISIBLE
                    durationsRow.visibility = View.VISIBLE
                    durationsRow.alpha = 0f
                    durationsRow.animate().alpha(1f).setDuration(FADE_IN_MILLIS).start()
                }
            })
            start()
        }
    }

    override fun onDestroy() {
        animator?.cancel()
        animator = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_LABEL = "label"

        /** The usage durations the user can pick after the breathing pause. */
        private val DURATION_MINUTES = intArrayOf(1, 2, 5, 10, 15)

        /** One breath in or out; four of them make the pause about 14 seconds. */
        private const val BREATH_MILLIS = 3_500L
        private const val BREATH_REPEATS = 3
        private const val BREATH_MIN_SCALE = 0.6f
        private const val BREATH_MIN_ALPHA = 0.4f
        private const val FADE_IN_MILLIS = 400L

        fun intent(context: Context, pkg: String, label: String): Intent =
            Intent(context, PauseActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_PACKAGE, pkg)
                .putExtra(EXTRA_LABEL, label)
    }
}
