package io.github.minilauncher.ui.common

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import io.github.minilauncher.R
import io.github.minilauncher.blocking.StrictGuard
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.util.Time
import java.time.LocalDateTime
import java.util.Date

/** Applies [StrictGuard] to the screens: loosening waits until the session or schedule ends. */
object StrictModeGate {

    fun currentLock(context: Context): StrictGuard.Lock? {
        val prefs = Prefs.get(context)
        val now = LocalDateTime.now()
        return StrictGuard.lock(
            strictMode = prefs.strictMode,
            focusSessionUntil = prefs.focusSessionUntil,
            schedules = prefs.schedules,
            nowMillis = System.currentTimeMillis(),
            dayOfWeek = now.dayOfWeek.value,
            minuteOfDay = now.hour * Time.MINUTES_PER_HOUR + now.minute,
        )
    }

    /** Runs [action], which loosens blocking, unless strict mode holds it back; then says until when. */
    fun loosen(context: Context, action: () -> Unit) {
        val lock = currentLock(context)
        if (lock == null) {
            action()
            return
        }
        val until = when (lock) {
            is StrictGuard.Lock.Session -> DateFormat.getTimeFormat(context).format(Date(lock.untilMillis))
            is StrictGuard.Lock.ScheduleWindow -> Time.formatMinuteOfDay(lock.endMinute)
        }
        Toast.makeText(context, context.getString(R.string.strict_locked, until), Toast.LENGTH_LONG).show()
    }
}
