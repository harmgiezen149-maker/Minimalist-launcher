package io.github.minilauncher.blocking

import io.github.minilauncher.blocking.WindowGate.Window
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowGateTest {

    private fun check(windowId: Int, windows: List<Window>?, activePackage: String? = null) =
        WindowGate.isActiveAppWindow(windowId, "com.app", windows) { activePackage }

    @Test
    fun `the active application window counts`() {
        assertTrue(check(1, listOf(Window(1, isApplication = true, isActive = true))))
    }

    @Test
    fun `an app window behind the task switcher does not count`() {
        val windows = listOf(
            Window(1, isApplication = true, isActive = false),
            Window(2, isApplication = false, isActive = true),
        )
        assertFalse(check(1, windows))
    }

    @Test
    fun `an active system window does not count`() {
        assertFalse(check(2, listOf(Window(2, isApplication = false, isActive = true))))
    }

    @Test
    fun `an event from an unknown window does not count`() {
        assertFalse(check(9, listOf(Window(1, isApplication = true, isActive = true))))
    }

    @Test
    fun `without a window list the foreground package decides`() {
        assertTrue(check(1, null, activePackage = "com.app"))
        assertTrue(check(1, emptyList(), activePackage = "com.app"))
        assertFalse(check(1, null, activePackage = "com.other"))
    }

    @Test
    fun `without any information nothing counts`() {
        assertFalse(check(1, null, activePackage = null))
    }

    @Test
    fun `the foreground package is not consulted when a window list exists`() {
        var asked = false
        WindowGate.isActiveAppWindow(1, "com.app", listOf(Window(1, isApplication = true, isActive = true))) {
            asked = true
            null
        }
        assertFalse(asked)
    }
}
