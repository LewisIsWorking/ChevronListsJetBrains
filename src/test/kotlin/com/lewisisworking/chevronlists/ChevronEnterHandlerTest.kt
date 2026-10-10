/**
 * ChevronEnterHandlerTest.kt
 * Presses Enter in a headless editor, so the handler's document edits are
 * tested as well as the pure decision in EnterContinuation.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ChevronEnterHandlerTest : BasePlatformTestCase() {

    private fun enter(text: String): String {
        myFixture.configureByText("notes.md", text)
        myFixture.type('\n')
        return myFixture.editor.document.text
    }

    fun `test Enter on a header above a numbered list adds item 1 and renumbers the rest`() =
        assertEquals("> Paid.\n>> 1. \n>> 2. Lewis.\n>> 3. Eimer.\n\n> Owed\n>> 1. Ethan.\n",
            enter("> Paid.<caret>\n>> 1. Lewis.\n>> 2. Eimer.\n\n> Owed\n>> 1. Ethan.\n"))

    fun `test Enter on a header above a bullet list adds a bullet`() =
        assertEquals("> Paid.\n>> - \n>> - Lewis.\n", enter("> Paid.<caret>\n>> - Lewis.\n"))

    fun `test Enter on a numbered item continues the numbering`() =
        assertEquals("> Paid.\n>> 1. Lewis.\n>> 2. \n", enter("> Paid.\n>> 1. Lewis.<caret>\n"))

    fun `test Enter mid-list renumbers the items after the new one`() =
        assertEquals("# Bikes\n> Paid.\n>> 1. Lewis.\n>> 2. \n>> 3. Eimer.\n>>> 1. note\n>> 4. Luke.\n",
            enter("# Bikes\n> Paid.\n>> 1. Lewis.<caret>\n>> 2. Eimer.\n>>> 1. note\n>> 3. Luke.\n"))
}
