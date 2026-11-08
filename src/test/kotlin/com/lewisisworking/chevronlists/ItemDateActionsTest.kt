/**
 * ItemDateActionsTest.kt
 * Runs the due date, date stamp, Fix Numbering and Select Section Items actions
 * in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.time.LocalDate

class ItemDateActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private val text get() = myFixture.editor.document.text

    fun `test set due date`() {
        myFixture.configureByText("notes.md", ">> - <caret>a +2")
        TestDialogManager.setTestInputDialog { "2026-12-31" }
        myFixture.performEditorAction("ChevronLists.SetDueDate")
        assertEquals(">> - a @2026-12-31 +2", text)
    }

    fun `test cancelling the due date changes nothing`() {
        myFixture.configureByText("notes.md", ">> - <caret>a")
        TestDialogManager.setTestInputDialog { null }
        myFixture.performEditorAction("ChevronLists.SetDueDate")
        assertEquals(">> - a", text)
    }

    fun `test a due date needs an item`() {
        myFixture.configureByText("notes.md", "> <caret>A")
        TestDialogManager.setTestInputDialog { error("should not ask") }
        myFixture.performEditorAction("ChevronLists.SetDueDate")
        assertEquals("> A", text)
    }

    fun `test the date stamp goes at every caret and replaces a selection`() {
        myFixture.configureByText("notes.md", "a<caret>\n<selection>old</selection>")
        myFixture.performEditorAction("ChevronLists.InsertDateStamp")
        val stamp = "@${LocalDate.now()}"
        assertEquals("a$stamp\n$stamp", text)
    }

    fun `test fix numbering`() {
        myFixture.configureByText("notes.md", "> A\n>> 3.<caret> a\n>> 9. b")
        myFixture.performEditorAction("ChevronLists.FixNumbering")
        assertEquals("> A\n>> 1. a\n>> 2. b", text)
    }

    fun `test select section items`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a\nnote\n>> - b\n> B\n>> - c")
        myFixture.performEditorAction("ChevronLists.SelectSectionItems")
        val selected = myFixture.editor.caretModel.allCarets.map { it.selectedText }
        assertEquals(listOf(">> - a", ">> - b"), selected)
    }
}
