/**
 * ItemAgeHintsTest.kt
 * Plain JUnit 4 tests for itemAgeHint.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class ItemAgeHintsTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test fun `hint is null without a created stamp`() {
        assertNull(itemAgeHint("Just words #tag", today))
    }

    @Test fun `hint is null for a fresh item`() {
        assertNull(itemAgeHint("Fresh @created:2026-11-10", today))
    }

    @Test fun `hint is null at 29 days`() {
        assertNull(itemAgeHint("Recent @created:2026-10-20", today))
    }

    @Test fun `hint shows at exactly 30 days`() {
        assertEquals("(30d old)", itemAgeHint("Old @created:2026-10-19", today))
    }

    @Test fun `hint shows the day count`() {
        assertEquals("(78d old)", itemAgeHint("Older @created:2026-09-01", today))
    }

    @Test fun `hint reads the stamp through markers`() {
        assertEquals("(78d old)", itemAgeHint("[x] !!! Done #work @created:2026-09-01", today))
    }
}
