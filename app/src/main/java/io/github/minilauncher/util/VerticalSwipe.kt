package io.github.minilauncher.util

import kotlin.math.abs

/**
 * Tells a deliberate vertical swipe apart from a scroll or a sideways flick.
 * Pure, so it is unit-tested on a plain JVM.
 */
object VerticalSwipe {

    enum class Direction { UP, DOWN, NONE }

    /** Minimum travel in dp, so short scroll flicks never count. */
    const val MIN_DISTANCE_DP = 100f

    /** Minimum speed in px/s, so a slow drag never counts. */
    const val MIN_VELOCITY = 1500f

    fun classify(dx: Float, dy: Float, velocityY: Float, density: Float): Direction {
        val vertical = abs(dy) >= abs(dx)
        val farEnough = abs(dy) >= MIN_DISTANCE_DP * density
        val fastEnough = abs(velocityY) >= MIN_VELOCITY
        return when {
            !vertical || !farEnough || !fastEnough -> Direction.NONE
            dy < 0 -> Direction.UP
            else -> Direction.DOWN
        }
    }
}
