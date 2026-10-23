package io.github.minilauncher.util

import io.github.minilauncher.util.VerticalSwipe.Direction
import org.junit.Assert.assertEquals
import org.junit.Test

class VerticalSwipeTest {

    private val density = 2f // 100dp = 200px

    @Test
    fun `fast long upward fling is a swipe up`() {
        assertEquals(Direction.UP, VerticalSwipe.classify(dx = 10f, dy = -300f, velocityY = -2000f, density))
    }

    @Test
    fun `fast long downward fling is a swipe down`() {
        assertEquals(Direction.DOWN, VerticalSwipe.classify(dx = 0f, dy = 300f, velocityY = 2000f, density))
    }

    @Test
    fun `too short does not count`() {
        assertEquals(Direction.NONE, VerticalSwipe.classify(dx = 0f, dy = 150f, velocityY = 3000f, density))
    }

    @Test
    fun `too slow does not count`() {
        assertEquals(Direction.NONE, VerticalSwipe.classify(dx = 0f, dy = 400f, velocityY = 1000f, density))
    }

    @Test
    fun `mostly sideways does not count`() {
        assertEquals(Direction.NONE, VerticalSwipe.classify(dx = 500f, dy = 300f, velocityY = 3000f, density))
    }

    @Test
    fun `exactly at the thresholds counts`() {
        assertEquals(Direction.DOWN, VerticalSwipe.classify(dx = 200f, dy = 200f, velocityY = 1500f, density))
    }
}
