package io.github.minilauncher.ui.common

import android.app.Activity
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Moves slow work (usage statistics, package-manager queries) off the main
 * thread, where it would stall the screen or, past five seconds, trigger an
 * "app isn't responding" dialog.
 */
object Async {

    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "launcher-worker").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())

    /** Runs [work] on the worker thread and [onResult] on the main thread. */
    fun <T> run(work: () -> T, onResult: (T) -> Unit) {
        worker.execute {
            val result = runCatching(work)
            main.post { result.onSuccess(onResult) }
        }
    }

    /** Like [run], but drops the result when [activity] has closed in the meantime. */
    fun <T> load(activity: Activity, work: () -> T, onResult: (T) -> Unit) {
        run(work) { result ->
            if (!activity.isFinishing && !activity.isDestroyed) onResult(result)
        }
    }
}
