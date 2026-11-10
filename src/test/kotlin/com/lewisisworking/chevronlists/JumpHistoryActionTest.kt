/**
 * JumpHistoryActionTest.kt
 * Runs Jump Back after header jumps in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JumpHistoryActionTest : BasePlatformTestCase() {

    private fun act(id: String) = myFixture.performEditorAction("ChevronLists.$id")
    private fun next() = act("JumpToNextHeader")
    private fun back() = act("JumpBack")
    private fun open(body: String) { myFixture.configureByText("notes.md", body) }
    private fun line() = myFixture.editor.caretModel.logicalPosition.line
    private fun column() = myFixture.editor.caretModel.logicalPosition.column

    fun `test jump back returns to the exact position`() {
        open("> A\n>> - wo<caret>rk\n> B\n")
        next()
        assertEquals(2, line())
        back()
        assertEquals(1, line())
        assertEquals(7, column())
    }

    fun `test jump back walks back through the jumps`() {
        open("> A<caret>\n> B\n> C\n")
        next()
        next()
        back()
        assertEquals(1, line())
        back()
        assertEquals(0, line())
    }

    fun `test a previous header jump is recorded too`() {
        open("> A\n> B\n>> - b<caret>\n")
        act("JumpToPreviousHeader")
        assertEquals(1, line())
        back()
        assertEquals(2, line())
    }

    fun `test jump back with no history stays put`() {
        open("> A\n>> - wo<caret>rk\n")
        back()
        assertEquals(1, line())
    }

    fun `test a jump that goes nowhere is not recorded`() {
        open("> A\n>> - wo<caret>rk\n")
        next()
        assertEquals(1, line())
        assertEquals(emptyList<Int>(), jumpOffsets(myFixture.editor.document))
    }

    fun `test only the last ten jumps are kept`() {
        open((0..11).joinToString("\n") { "> S$it" })
        repeat(11) { next() }
        assertEquals(11, line())
        repeat(12) { back() }
        assertEquals(1, line())
    }

    fun `test the jump follows its text when lines are added above`() {
        open("> A\n>> - wo<caret>rk\n> B\n")
        next()
        val document = myFixture.editor.document
        WriteCommandAction.runWriteCommandAction(project) { document.insertString(0, "> New\n\n") }
        back()
        assertEquals(3, line())
        assertEquals(7, column())
    }

    fun `test jumps are listed newest first`() {
        open("> A<caret>\n> B\n> C\n")
        next()
        next()
        val document = myFixture.editor.document
        assertEquals(listOf(document.getLineStartOffset(1), 0), jumpOffsets(document))
    }

    fun `test the history is per file`() {
        open("> A<caret>\n> B\n")
        next()
        myFixture.configureByText("other.md", "> C\n>> - c<caret>\n")
        back()
        assertEquals(1, line())
    }

    fun `test the reports run with no history`() {
        open("> A<caret>\n")
        act("ShowJumpHistory")
        act("ShowRecentSections")
        assertEquals(0, line())
    }
}
