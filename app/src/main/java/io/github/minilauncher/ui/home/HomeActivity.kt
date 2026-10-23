package io.github.minilauncher.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.minilauncher.R
import io.github.minilauncher.blocking.AppBlockerAccessibilityService
import io.github.minilauncher.blocking.BlockState
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.WeatherFetcher
import io.github.minilauncher.data.model.AppEntry
import io.github.minilauncher.mode.ModeState
import io.github.minilauncher.ui.common.AppLauncher
import io.github.minilauncher.ui.common.AppLongPressDialog
import io.github.minilauncher.ui.common.BaseActivity
import io.github.minilauncher.ui.common.EveningOverrideDialog
import io.github.minilauncher.ui.common.PermissionChecks
import io.github.minilauncher.ui.common.StrictModeGate
import io.github.minilauncher.ui.common.TextListAdapter
import io.github.minilauncher.ui.common.announceAsButtons
import io.github.minilauncher.ui.drawer.AppDrawerActivity
import io.github.minilauncher.ui.onboarding.OnboardingActivity
import io.github.minilauncher.ui.settings.SettingsActivity
import io.github.minilauncher.util.Time
import io.github.minilauncher.util.VerticalSwipe

class HomeActivity : BaseActivity() {

    override val followsPortraitLock = true

    private lateinit var repo: AppRepository
    private lateinit var prefs: Prefs
    private lateinit var adapter: TextListAdapter
    private lateinit var gestureDetector: GestureDetector
    private var favorites: List<AppEntry> = emptyList()

    /** Last rendered day/evening state, so the list is only rebuilt when it flips. */
    private var lastModeKey: String? = null

    private val background = java.util.concurrent.Executors.newSingleThreadExecutor()

    /**
     * The temperature has its own thread: on a slow network its fetch can
     * take seconds, and the favorites list must never queue behind it.
     */
    private val weather = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val countdownTick = object : Runnable {
        override fun run() {
            updateFocusCountdown()
            syncMode()
            handler.postDelayed(this, TICK_MILLIS)
        }
    }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) updateTemperature()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        repo = AppRepository(this)
        prefs = Prefs.get(this)

        adapter = TextListAdapter(
            onClick = { pos -> favorites.getOrNull(pos)?.let { AppLauncher.launch(this, it.packageName) } },
            onLongClick = { pos ->
                favorites.getOrNull(pos)?.let { entry ->
                    AppLongPressDialog.show(this, entry) { refresh() }
                }
            },
        )
        findViewById<RecyclerView>(R.id.favoritesList).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
        }

        setUpButtons()
        setUpGestures()

        // Home screen: back does nothing. A callback, because Android 16 no
        // longer calls onBackPressed for apps that target it.
        onBackPressedDispatcher.addCallback(this) {}

        if (!prefs.onboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    /** Every tappable text on the home screen. */
    private fun setUpButtons() {
        findViewById<TextView>(R.id.allAppsButton).setOnClickListener { openDrawer() }
        findViewById<TextView>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<TextView>(R.id.blockerWarning).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        findViewById<TextView>(R.id.focusSessionCountdown).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.focus_session_cancel_title)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    StrictModeGate.loosen(this) {
                        prefs.focusSessionUntil = 0L
                        updateFocusCountdown()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        announceAsButtons(
            window.decorView,
            R.id.temperature,
            R.id.modeLabel,
            R.id.focusSessionCountdown,
            R.id.blockerWarning,
            R.id.shortcutLeft,
            R.id.allAppsButton,
            R.id.settingsButton,
            R.id.shortcutRight,
        )
        setUpShortcut(R.id.shortcutLeft, isLeft = true)
        setUpShortcut(R.id.shortcutRight, isLeft = false)

        findViewById<TextView>(R.id.modeLabel).setOnClickListener {
            EveningOverrideDialog.show(this) { refresh() }
        }

        findViewById<TextView>(R.id.temperature).setOnClickListener {
            if (!hasLocationPermission()) {
                locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                updateTemperature()
            }
        }
    }

    /** Swipe up opens the drawer, swipe down the notification shade. */
    private fun setUpGestures() {
        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (e1 == null) return false
                    val direction = VerticalSwipe.classify(
                        dx = e2.x - e1.x,
                        dy = e2.y - e1.y,
                        velocityY = velocityY,
                        density = resources.displayMetrics.density,
                    )
                    when (direction) {
                        // Only open the drawer when the list has no more to scroll
                        VerticalSwipe.Direction.UP ->
                            if (!findViewById<RecyclerView>(R.id.favoritesList).canScrollVertically(1)) openDrawer()
                        VerticalSwipe.Direction.DOWN -> AppBlockerAccessibilityService.openNotificationShade()
                        VerticalSwipe.Direction.NONE -> return false
                    }
                    return true
                }
            },
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        // Fallback path: the accessibility service forced us home because a
        // blocked app opened, but could not start the block screen itself.
        BlockState.consumePendingBlock()?.let { info ->
            startActivity(AppLauncher.blockIntent(this, info, repo.labelFor(info.packageName)))
        }
        handler.post(countdownTick)
        updateTemperature()
        refresh()
    }

    override fun onPause() {
        handler.removeCallbacks(countdownTick)
        super.onPause()
    }

    override fun onDestroy() {
        background.shutdown()
        weather.shutdown()
        super.onDestroy()
    }

    private fun openDrawer() {
        startActivity(Intent(this, AppDrawerActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
    }

    // ---- temperature ----

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun bestLastLocation(): Location? {
        if (!hasLocationPermission()) return null
        val lm = getSystemService(LocationManager::class.java) ?: return null
        return listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider ->
            // The permission can be revoked between the check above and here.
            try {
                lm.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } catch (e: IllegalArgumentException) {
                null // provider not present on this device
            }
        }.maxByOrNull { it.time }
    }

    private fun updateTemperature() {
        val view = findViewById<TextView>(R.id.temperature)
        val known = WeatherFetcher.lastKnown()
        view.text = if (known != null) {
            getString(R.string.home_temp, known)
        } else {
            getString(R.string.home_temp_unknown)
        }
        if (WeatherFetcher.isFresh()) return
        // Both the location lookup and the fetch are off the main thread:
        // getLastKnownLocation is a system call that can take a while.
        weather.execute {
            val location = bestLastLocation() ?: return@execute
            val temp = WeatherFetcher.fetch(location.latitude, location.longitude) ?: return@execute
            runOnUiThread {
                if (!isDestroyed) view.text = getString(R.string.home_temp, temp)
            }
        }
    }

    private fun setUpShortcut(viewId: Int, isLeft: Boolean) {
        val view = findViewById<TextView>(viewId)
        view.setOnClickListener {
            when (val target = if (isLeft) prefs.shortcutLeft else prefs.shortcutRight) {
                Prefs.SHORTCUT_DIALER -> runCatching { startActivity(Intent(Intent.ACTION_DIAL)) }
                else -> if (repo.isLaunchable(target)) AppLauncher.launch(this, target) else showShortcutPicker(isLeft)
            }
        }
        view.setOnLongClickListener {
            showShortcutPicker(isLeft)
            true
        }
    }

    private fun showShortcutPicker(isLeft: Boolean) {
        val apps = repo.allApps(includeHidden = true, respectMode = false)
        val labels = mutableListOf(getString(R.string.shortcut_dialer))
        labels += apps.map { it.displayLabel }
        AlertDialog.Builder(this)
            .setTitle(R.string.shortcut_pick_title)
            .setItems(labels.toTypedArray()) { _, which ->
                val value = if (which == 0) Prefs.SHORTCUT_DIALER else apps[which - 1].packageName
                if (isLeft) prefs.shortcutLeft = value else prefs.shortcutRight = value
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** A shortcut to an app that is not installed (Gmail, by default) invites a choice instead. */
    private fun shortcutLabel(target: String): String = when {
        target == Prefs.SHORTCUT_DIALER -> getString(R.string.shortcut_dialer_label)
        repo.isLaunchable(target) -> repo.labelFor(target)
        else -> getString(R.string.shortcut_choose)
    }

    // ---- day / evening mode ----

    /** Rebuilds the app list when the mode flipped; otherwise just refreshes the label. */
    private fun syncMode() {
        val state = prefs.currentModeState()
        val key = "${state.enabled}-${state.evening}"
        if (key != lastModeKey) {
            lastModeKey = key
            refresh()
        } else {
            updateModeLabel(state)
        }
    }

    private fun updateModeLabel(state: ModeState = prefs.currentModeState()) {
        val view = findViewById<TextView>(R.id.modeLabel)
        if (!state.enabled) {
            view.visibility = View.GONE
            return
        }
        view.visibility = View.VISIBLE
        view.text = when {
            state.overrideActive ->
                getString(R.string.home_mode_evening_override, state.overrideRemainingMinutes)
            state.evening -> getString(R.string.home_mode_evening)
            else -> getString(R.string.home_mode_day)
        }
        // Evening gets the brighter foreground as its subtle marker.
        val attr = if (state.evening) {
            android.R.attr.textColorPrimary
        } else {
            android.R.attr.textColorSecondary
        }
        val styled = theme.obtainStyledAttributes(intArrayOf(attr))
        view.setTextColor(styled.getColor(0, view.currentTextColor))
        styled.recycle()
    }

    private fun updateFocusCountdown() {
        val view = findViewById<TextView>(R.id.focusSessionCountdown)
        val until = prefs.focusSessionUntil
        val now = System.currentTimeMillis()
        if (until > now) {
            val minutes = Time.minutesUntil(until, now)
            view.text = getString(R.string.home_focus_countdown, minutes)
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    /**
     * Reads preferences only — safe to run on the main thread.
     */
    private fun refreshInstantUi() {
        updateFocusCountdown()
        val state = prefs.currentModeState()
        lastModeKey = "${state.enabled}-${state.evening}"
        updateModeLabel(state)
    }

    /**
     * Everything that queries the package manager runs off the main thread.
     * Doing it inside onResume stalls the very frame the system needs for the
     * swipe-up animation, which makes it cancel the task switcher.
     */
    private fun refresh() {
        refreshInstantUi()
        background.execute {
            val favs = runCatching { repo.favoriteApps() }.getOrDefault(emptyList())
            val leftLabel = shortcutLabel(prefs.shortcutLeft)
            val rightLabel = shortcutLabel(prefs.shortcutRight)
            val warnBlocker = PermissionChecks.blockerConfiguredButDisabled(this)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                favorites = favs
                adapter.submit(favs.map { it.displayLabel })
                findViewById<TextView>(R.id.emptyHint).visibility =
                    if (favs.isEmpty()) View.VISIBLE else View.GONE
                findViewById<TextView>(R.id.blockerWarning).visibility =
                    if (warnBlocker) View.VISIBLE else View.GONE
                findViewById<TextView>(R.id.shortcutLeft).text = leftLabel
                findViewById<TextView>(R.id.shortcutRight).text = rightLabel
            }
        }
    }

    private companion object {
        /** How often the focus countdown and the day/evening label refresh. */
        const val TICK_MILLIS = 30_000L
    }
}
