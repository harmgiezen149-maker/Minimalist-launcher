package io.github.minilauncher.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.github.minilauncher.R
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.model.Schedule
import io.github.minilauncher.ui.common.BaseActivity
import io.github.minilauncher.ui.common.StrictModeGate
import io.github.minilauncher.ui.common.announceAsButtons
import io.github.minilauncher.util.Time
import java.time.DayOfWeek
import java.util.UUID

/** List, create, rename, switch, edit and delete blocking schedules. */
class ScheduleEditorActivity : BaseActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedules)
        prefs = Prefs.get(this)
        announceAsButtons(window.decorView, R.id.addScheduleButton)
        findViewById<TextView>(R.id.addScheduleButton).setOnClickListener {
            pickDays(
                Schedule(
                    id = UUID.randomUUID().toString(),
                    label = "",
                    days = WORKDAYS,
                    startMinute = DEFAULT_START,
                    endMinute = DEFAULT_END,
                    enabled = true,
                ),
                isNew = true,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val container = findViewById<LinearLayout>(R.id.schedulesContainer)
        container.removeAllViews()
        val schedules = prefs.schedules
        if (schedules.isEmpty()) {
            val view = layoutInflater.inflate(R.layout.item_app_text, container, false) as TextView
            view.text = getString(R.string.schedules_empty)
            container.addView(view)
        }
        schedules.forEach { schedule ->
            val view = layoutInflater.inflate(R.layout.item_setting_row, container, false)
            view.findViewById<TextView>(R.id.rowTitle).text = describe(schedule)
            view.findViewById<TextView>(R.id.rowValue).text =
                if (schedule.enabled) getString(R.string.state_on) else getString(R.string.state_off)
            view.setOnClickListener { showMenu(schedule) }
            container.addView(view)
        }
    }

    private fun describe(s: Schedule): String {
        val dayNames = resources.getStringArray(R.array.day_names_short)
        val days = s.days.sorted().joinToString(" ") { dayNames[it - 1] }
        val name = s.label.ifBlank { getString(R.string.schedule_default_name) }
        return "$name · $days · ${Time.formatMinuteOfDay(s.startMinute)}–${Time.formatMinuteOfDay(s.endMinute)}"
    }

    /**
     * Renaming is always possible; changing times, switching off and deleting
     * can loosen blocking, so strict mode gates them.
     */
    private fun showMenu(schedule: Schedule) {
        val actions = listOf(
            getString(R.string.schedule_edit_times) to {
                StrictModeGate.loosen(this) { pickDays(schedule, isNew = false) }
            },
            getString(R.string.schedule_rename) to { rename(schedule) },
            getString(if (schedule.enabled) R.string.schedule_turn_off else R.string.schedule_turn_on) to {
                val toggle = { save(schedule.copy(enabled = !schedule.enabled), isNew = false) }
                if (schedule.enabled) StrictModeGate.loosen(this, toggle) else toggle()
            },
            getString(R.string.schedule_delete) to {
                StrictModeGate.loosen(this) {
                    prefs.schedules = prefs.schedules.filterNot { it.id == schedule.id }
                    render()
                }
            },
        )
        AlertDialog.Builder(this)
            .setTitle(describe(schedule))
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun rename(schedule: Schedule) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = getString(R.string.schedule_default_name)
            setText(schedule.label)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.schedule_rename)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                save(schedule.copy(label = input.text.toString().trim()), isNew = false)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** First step of creating or editing: the days. At least one is required. */
    private fun pickDays(schedule: Schedule, isNew: Boolean) {
        val dayNames = resources.getStringArray(R.array.day_names)
        val checked = BooleanArray(DayOfWeek.entries.size) { (it + 1) in schedule.days }
        AlertDialog.Builder(this)
            .setTitle(if (isNew) R.string.schedule_new else R.string.schedule_edit)
            .setMultiChoiceItems(dayNames, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton(R.string.schedule_save) { _, _ ->
                val days = buildSet { checked.forEachIndexed { i, on -> if (on) add(i + 1) } }
                if (days.isEmpty()) {
                    // A schedule without days would be saved but never be active.
                    Toast.makeText(this, R.string.schedule_no_days, Toast.LENGTH_LONG).show()
                    pickDays(schedule, isNew)
                } else {
                    pickTimes(schedule.copy(days = days), isNew)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pickTimes(schedule: Schedule, isNew: Boolean) {
        val perHour = Time.MINUTES_PER_HOUR
        TimePickerDialog(
            this,
            { _, startHour, startMin ->
                TimePickerDialog(
                    this,
                    { _, endHour, endMin ->
                        save(
                            schedule.copy(
                                startMinute = startHour * perHour + startMin,
                                endMinute = endHour * perHour + endMin,
                            ),
                            isNew,
                        )
                    },
                    schedule.endMinute / perHour,
                    schedule.endMinute % perHour,
                    true,
                ).apply { setTitle(getString(R.string.schedule_end_time)) }.show()
            },
            schedule.startMinute / perHour,
            schedule.startMinute % perHour,
            true,
        ).apply { setTitle(getString(R.string.schedule_start_time)) }.show()
    }

    private fun save(schedule: Schedule, isNew: Boolean) {
        prefs.schedules = if (isNew) {
            prefs.schedules + schedule
        } else {
            prefs.schedules.map { if (it.id == schedule.id) schedule else it }
        }
        render()
    }

    private companion object {
        val WORKDAYS = (DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value).toSet()
        const val DEFAULT_START = 9 * Time.MINUTES_PER_HOUR
        const val DEFAULT_END = 17 * Time.MINUTES_PER_HOUR
    }
}
