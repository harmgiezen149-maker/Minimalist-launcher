package io.github.minilauncher.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputRulesTest {

    @Test
    fun `accepts a normal limit`() {
        assertEquals(30, InputRules.parseLimitMinutes("30"))
        assertEquals(45, InputRules.parseLimitMinutes(" 45 "))
    }

    @Test
    fun `accepts the boundaries`() {
        assertEquals(1, InputRules.parseLimitMinutes("1"))
        assertEquals(1440, InputRules.parseLimitMinutes("1440"))
    }

    @Test
    fun `rejects zero, negative, too large, empty and non-numbers`() {
        listOf("0", "-5", "1441", "99999999999", "", "  ", "ten", "1.5").forEach {
            assertNull("\"$it\" should be rejected", InputRules.parseLimitMinutes(it))
        }
    }
}
