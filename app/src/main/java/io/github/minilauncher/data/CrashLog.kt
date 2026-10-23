package io.github.minilauncher.data

import android.content.Context
import io.github.minilauncher.BuildConfig
import io.github.minilauncher.util.CrashReport
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the last crash on this phone so a problem can be looked into
 * afterwards (Settings › Last crash). Nothing is sent anywhere; the file is
 * excluded from backups and replaced by the next crash.
 */
object CrashLog {

    private const val FILE_NAME = "last_crash.txt"

    /** Records uncaught exceptions, then lets Android handle them as before. */
    fun install(context: Context) {
        val file = file(context)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT})"
                file.writeText(CrashReport.format(time, version, thread.name, error))
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? = file(context).takeIf { it.isFile }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)
}
