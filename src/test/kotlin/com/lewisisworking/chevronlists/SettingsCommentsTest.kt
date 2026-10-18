/**
 * SettingsCommentsTest.kt
 * Guards the Settings page layout: every comment wraps short enough to fit the pane.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class SettingsCommentsTest {

    @Test fun `settings comments wrap instead of running off the pane`() {
        // A plain .comment() wraps at the default length: in a narrow Settings pane
        // the long ones ran past the right edge and added a sideways scrollbar
        val source = File("src/main/kotlin/com/lewisisworking/chevronlists/ChevronListsConfigurable.kt").readText()
        val plain = Regex("""\.comment\(""").findAll(source).count()
        assertEquals(0, plain)
        assertTrue(source.contains("comment(text, maxLineLength = COMMENT_LINE_LENGTH)"))
        assertTrue(COMMENT_LINE_LENGTH in 1..50)
        assertTrue(Regex("""\.wrappedComment\(""").findAll(source).count() >= 8)
    }
}
