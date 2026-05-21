/**
 * ColourLabelsTest.kt
 * Plain JUnit 4 tests for the colour-label pure functions.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ColourLabelsTest {

    // enum + token format
    @Test fun `all six labels exist with correct hex`() {
        assertEquals("#E06C75", ColourLabel.RED.hex)
        assertEquals("#98C379", ColourLabel.GREEN.hex)
        assertEquals("#61AFEF", ColourLabel.BLUE.hex)
        assertEquals("#E5C07B", ColourLabel.YELLOW.hex)
        assertEquals("#FF9800", ColourLabel.ORANGE.hex)
        assertEquals("#A052E5", ColourLabel.PURPLE.hex)
    }
    @Test fun `token property builds curly form`() {
        assertEquals("{red}",   ColourLabel.RED.token)
        assertEquals("{green}", ColourLabel.GREEN.token)
    }

    // parseColourLabel
    @Test fun `parses the first label in content`() {
        assertEquals(ColourLabel.RED, parseColourLabel("{red} something"))
    }
    @Test fun `parses label not at the start`() {
        assertEquals(ColourLabel.BLUE, parseColourLabel("Fix the bug {blue}"))
    }
    @Test fun `returns null when no label present`() {
        assertNull(parseColourLabel("Just plain text"))
    }
    @Test fun `returns first label when multiple present`() {
        assertEquals(ColourLabel.GREEN, parseColourLabel("{green} task {red}"))
    }
    @Test fun `unknown colour name is not a label`() {
        assertNull(parseColourLabel("{cyan} not a label"))
    }

    // removeColourLabel
    @Test fun `strips a single label and collapses whitespace`() {
        assertEquals("Task", removeColourLabel("{red} Task"))
    }
    @Test fun `strips multiple labels and trims`() {
        assertEquals("Task", removeColourLabel("{red} {green} Task"))
    }
    @Test fun `leaves clean content unchanged`() {
        assertEquals("Plain text", removeColourLabel("Plain text"))
    }

    // setColourLabel
    @Test fun `prepends a label to empty content`() {
        assertEquals("{red}", setColourLabel("", ColourLabel.RED))
    }
    @Test fun `prepends a label to plain content`() {
        assertEquals("{green} Task", setColourLabel("Task", ColourLabel.GREEN))
    }
    @Test fun `replaces an existing label`() {
        assertEquals("{blue} Task", setColourLabel("{red} Task", ColourLabel.BLUE))
    }
    @Test fun `replacing strips ALL existing labels first`() {
        assertEquals("{purple} Task", setColourLabel("{red} {green} Task", ColourLabel.PURPLE))
    }

    // findColourLabels
    @Test fun `finds all label ranges on a line`() {
        val results = findColourLabels(">> - {red} Task {green} again")
        assertEquals(2, results.size)
        assertEquals(ColourLabel.RED,   results[0].second)
        assertEquals(ColourLabel.GREEN, results[1].second)
    }
    @Test fun `returns empty list when no labels found`() {
        assertTrue(findColourLabels(">> - Plain item").isEmpty())
    }

    // computeSetColourLabel
    @Test fun `sets label on bullet item preserving chevrons and prefix`() {
        assertEquals(">>> - {red} Task", computeSetColourLabel(">>> - Task", "-", ColourLabel.RED))
    }
    @Test fun `sets label on numbered item preserving chevrons and number`() {
        assertEquals(">> 5. {blue} Task", computeSetColourLabel(">> 5. Task", "-", ColourLabel.BLUE))
    }
    @Test fun `replaces existing label on bullet item`() {
        assertEquals(">> - {green} Task", computeSetColourLabel(">> - {red} Task", "-", ColourLabel.GREEN))
    }
    @Test fun `set returns null for a header`() {
        assertNull(computeSetColourLabel("> Section", "-", ColourLabel.RED))
    }
    @Test fun `set returns null for plain text`() {
        assertNull(computeSetColourLabel("Just text", "-", ColourLabel.RED))
    }

    // computeRemoveColourLabel
    @Test fun `removes label from bullet item`() {
        assertEquals(">> - Task", computeRemoveColourLabel(">> - {red} Task", "-"))
    }
    @Test fun `removes label from numbered item`() {
        assertEquals(">> 3. Task", computeRemoveColourLabel(">> 3. {blue} Task", "-"))
    }
    @Test fun `remove returns null when item has no label`() {
        assertNull(computeRemoveColourLabel(">> - Task", "-"))
    }
    @Test fun `remove returns null for a header`() {
        assertNull(computeRemoveColourLabel("> Section", "-"))
    }
    @Test fun `remove preserves item markers around the removed label`() {
        assertEquals(">> - ⭐ Important Task",
                     computeRemoveColourLabel(">> - {red} ⭐ Important Task", "-"))
    }
}