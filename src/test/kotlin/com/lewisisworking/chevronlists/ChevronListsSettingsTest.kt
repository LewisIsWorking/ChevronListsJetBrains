/**
 * ChevronListsSettingsTest.kt
 * Plain JUnit 4 tests for the ChevronListsSettings.State data class.
 * Verifies defaults match the VS Code extension and that values round-trip.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ChevronListsSettingsTest {

    @Test fun `default listPrefix is dash`() {
        assertEquals("-", ChevronListsSettings.State().listPrefix)
    }

    @Test fun `default defaultNewListType is unordered`() {
        assertEquals("unordered", ChevronListsSettings.State().defaultNewListType)
    }

    @Test fun `default autoFixNumbering is true`() {
        assertTrue(ChevronListsSettings.State().autoFixNumbering)
    }

    @Test fun `default colourPreset is default`() {
        assertEquals("default", ChevronListsSettings.State().colourPreset)
    }

    @Test fun `state values are mutable`() {
        val s = ChevronListsSettings.State()
        s.listPrefix = "*"
        s.defaultNewListType = "ordered"
        assertEquals("*", s.listPrefix)
        assertEquals("ordered", s.defaultNewListType)
    }

    @Test fun `state equality compares all fields`() {
        val a = ChevronListsSettings.State(listPrefix = "-", defaultNewListType = "ordered")
        val b = ChevronListsSettings.State(listPrefix = "-", defaultNewListType = "ordered")
        assertEquals(a, b)
    }

    @Test fun `state inequality on listPrefix`() {
        val a = ChevronListsSettings.State(listPrefix = "-")
        val b = ChevronListsSettings.State(listPrefix = "*")
        assertNotEquals(a, b)
    }

    @Test fun `state inequality on defaultNewListType`() {
        val a = ChevronListsSettings.State(defaultNewListType = "unordered")
        val b = ChevronListsSettings.State(defaultNewListType = "ordered")
        assertNotEquals(a, b)
    }
}