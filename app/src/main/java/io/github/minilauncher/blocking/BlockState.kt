package io.github.minilauncher.blocking

import io.github.minilauncher.data.model.BlockedInfo

/**
 * In-process handoff between the accessibility service and the UI. If the
 * service cannot start BlockedActivity directly (background-launch
 * restrictions), HomeActivity picks this up in onResume after the forced
 * return home.
 */
object BlockState {

    @Volatile
    var pendingBlock: BlockedInfo? = null
        private set

    /** Leaves [info] for the home screen in case the service's own start of the block screen fails. */
    @Synchronized
    fun post(info: BlockedInfo) {
        pendingBlock = info
    }

    @Synchronized
    fun consumePendingBlock(): BlockedInfo? {
        val info = pendingBlock
        pendingBlock = null
        return info
    }

    /**
     * Called by the block and pause screens once they show for [packageName]:
     * the fallback is no longer needed, and leaving it would open the same
     * screen a second time when the user returns home.
     */
    @Synchronized
    fun clearFor(packageName: String) {
        if (pendingBlock?.packageName == packageName) pendingBlock = null
    }
}
