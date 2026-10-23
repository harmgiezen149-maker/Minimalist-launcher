package io.github.minilauncher.data.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {

    private val schedule = Schedule("id-1", "Work", setOf(1, 2, 3, 4, 5), 9 * 60, 17 * 60, enabled = true)

    @Test
    fun `round trips through json`() {
        assertEquals(schedule, Schedule.fromJson(schedule.toJson()))
    }

    @Test
    fun `disabled overnight schedule round trips`() {
        val night = Schedule("id-2", "", setOf(7), 22 * 60, 6 * 60, enabled = false)
        assertEquals(night, Schedule.fromJson(night.toJson()))
    }

    @Test
    fun `older entries without label or enabled flag load with defaults`() {
        val json = JSONObject(schedule.toJson().toString()).apply {
            remove("label")
            remove("enabled")
        }
        val restored = Schedule.fromJson(json)
        assertEquals("", restored.label)
        assertTrue(restored.enabled)
    }

    @Test
    fun `days are stored sorted`() {
        val shuffled = schedule.copy(days = linkedSetOf(5, 1, 3))
        assertEquals("[1,3,5]", shuffled.toJson().getJSONArray("days").toString())
    }
}
