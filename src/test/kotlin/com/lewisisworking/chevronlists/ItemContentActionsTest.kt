/**
 * ItemContentActionsTest.kt
 * Runs the item content actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestInputDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ItemContentActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private val text get() = myFixture.editor.document.text

    private fun perform(id: String, initial: String, vararg answers: String?): String {
        myFixture.configureByText("notes.md", initial)
        val queue = ArrayDeque(answers.toList())
        TestDialogManager.setTestInputDialog { queue.removeFirst() }
        myFixture.performEditorAction("ChevronLists.$id")
        return text
    }

    fun `test edit item content`() =
        assertEquals(">> - [x] b #t", perform("EditItemContent", ">> - <caret>[x] a #t", "b"))

    fun `test cancelling an edit changes nothing`() =
        assertEquals(">> - a", perform("EditItemContent", ">> - <caret>a", null))

    fun `test add quick note`() =
        assertEquals(">> - a // ring", perform("AddQuickNote", ">> - <caret>a", "ring"))

    fun `test convert item to section link`() =
        assertEquals("> Plan\n>> - [[Plan]]", perform("ConvertItemToSectionLink", "> Plan\n>> - <caret>plan"))

    fun `test replace in section`() =
        assertEquals("> A\n>> - axb", perform("ReplaceInSection", "> A\n>> - <caret>a-b", "-", "x"))

    fun `test an empty replacement deletes the text`() =
        assertEquals("> A\n>> - ab", perform("ReplaceInSection", "> A\n>> - <caret>a-b", "-", ""))

    fun `test shift all due dates`() =
        assertEquals("> A\n>> - a @2026-01-08", perform("ShiftAllDueDates", "> A\n>> - <caret>a @2026-01-01", "7"))

    fun `test insert group divider`() {
        assertEquals("> A\n>> -- Act One\n>> - b", perform("InsertGroupDivider", "> <caret>A\n>> - b", " Act One "))
        assertEquals(2, myFixture.editor.caretModel.logicalPosition.line)
        assertEquals(0, myFixture.editor.caretModel.logicalPosition.column)
    }

    fun `test a group divider on the last line leaves the caret at its end`() {
        assertEquals("> A\n>> -- B", perform("InsertGroupDivider", "> <caret>A", "B"))
        assertEquals(1, myFixture.editor.caretModel.logicalPosition.line)
        assertEquals(7, myFixture.editor.caretModel.logicalPosition.column)
    }
}
