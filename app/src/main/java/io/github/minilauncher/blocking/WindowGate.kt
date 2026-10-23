package io.github.minilauncher.blocking

/**
 * Decides whether an accessibility event comes from the application window
 * the user is actually using, rather than one sitting behind the task
 * switcher. Uses only window metadata, never the node tree, which is
 * expensive to build. Pure, so it is unit-tested on a plain JVM.
 */
object WindowGate {

    /** The parts of an AccessibilityWindowInfo this decision needs. */
    data class Window(val id: Int, val isApplication: Boolean, val isActive: Boolean)

    /**
     * @param windows the current window list, or null/empty when the system
     *   does not provide one
     * @param activePackage package of the active window's root; only asked
     *   for when there is no window list
     */
    fun isActiveAppWindow(
        eventWindowId: Int,
        eventPackage: String,
        windows: List<Window>?,
        activePackage: () -> String?,
    ): Boolean {
        if (!windows.isNullOrEmpty()) {
            val window = windows.firstOrNull { it.id == eventWindowId } ?: return false
            return window.isApplication && window.isActive
        }
        // No window list (the flag is not honoured on this device): fall back to
        // the foreground window's package. Refusing when that is unavailable is
        // deliberate — acting on a window the user is not in kicked people out
        // of the task switcher.
        return activePackage() == eventPackage
    }
}
