/**
 * ColourPresetsTest.kt
 * Plain JUnit 4 tests for the colour-preset data and resolver.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ColourPresetsTest {

    @Test fun `presets list contains all 13 expected ids`() {
        val ids = COLOUR_PRESETS.map { it.id }.toSet()
        assertEquals(
            setOf("default", "classic", "ocean", "forest", "sunset",
                  "monochrome", "midnight", "rose", "autumn",
                  "arctic", "neon", "sepia", "custom"),
            ids
        )
    }

    @Test fun `default preset uses violet header colour matching plugin icon`() {
        val p = findPreset("default")
        assertEquals("#A855F7", p.header.foreground)
        assertTrue(p.header.bold)
    }

    @Test fun `classic preset uses amber matching the original VS Code default`() {
        val p = findPreset("classic")
        assertEquals("#E5C07B", p.header.foreground)
    }

    @Test fun `unknown preset id falls back to default`() {
        assertEquals("default", findPreset("nonexistent").id)
    }

    @Test fun `custom preset has empty token colours`() {
        val p = findPreset("custom")
        assertNull(p.header.foreground)
        assertNull(p.prefix.foreground)
        assertNull(p.number.foreground)
    }

    @Test fun `isCustomPreset only true for the custom id`() {
        assertTrue(isCustomPreset("custom"))
        assertFalse(isCustomPreset("default"))
        assertFalse(isCustomPreset("ocean"))
    }

    @Test fun `resolvePresetAttributes returns null for the custom preset`() {
        assertNull(resolvePresetAttributes("custom", PresetToken.HEADER))
        assertNull(resolvePresetAttributes("custom", PresetToken.PREFIX))
        assertNull(resolvePresetAttributes("custom", PresetToken.NUMBER))
    }

    @Test fun `resolvePresetAttributes returns concrete attributes for a named preset`() {
        val attrs = resolvePresetAttributes("default", PresetToken.HEADER)
        assertNotNull(attrs)
        assertNotNull(attrs!!.foregroundColor)
    }

    @Test fun `every non-custom preset has all three token foregrounds set`() {
        for (preset in COLOUR_PRESETS.filterNot { isCustomPreset(it.id) }) {
            assertNotNull("Preset ${preset.id} missing header colour", preset.header.foreground)
            assertNotNull("Preset ${preset.id} missing prefix colour", preset.prefix.foreground)
            assertNotNull("Preset ${preset.id} missing number colour", preset.number.foreground)
        }
    }

    @Test fun `non-custom presets all use bold headers`() {
        for (preset in COLOUR_PRESETS.filterNot { isCustomPreset(it.id) }) {
            assertTrue("Preset ${preset.id} should have bold header", preset.header.bold)
        }
    }
}