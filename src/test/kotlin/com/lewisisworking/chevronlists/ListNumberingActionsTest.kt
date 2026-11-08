/**
 * ListNumberingActionsTest.kt
 * Runs the list numbering actions and Wrap Item Text in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ListNumberingActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
            TestDialogManager.setTestDialog(TestDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private val text get() = myFixture.editor.document.text

    fun `test rebase list from here`() {
        myFixture.configureByText("notes.md", "> A\n>> 5. a\n>> 9<caret>. b")
        myFixture.performEditorAction("ChevronLists.RebaseListFromHere")
        assertEquals("> A\n>> 5. a\n>> 6. b", text)
    }

    fun `test offset list numbers`() {
        myFixture.configureByText("notes.md", "> A\n>> 1<caret>. a\n>> 2. b")
        TestDialogManager.setTestInputDialog { "10" }
        myFixture.performEditorAction("ChevronLists.OffsetListNumbers")
        assertEquals("> A\n>> 11. a\n>> 12. b", text)
    }

    fun `test an offset below 1 changes nothing`() {
        myFixture.configureByText("notes.md", "> A\n>> 1<caret>. a")
        TestDialogManager.setTestInputDialog { "-5" }
        myFixture.performEditorAction("ChevronLists.OffsetListNumbers")
        assertEquals("> A\n>> 1. a", text)
    }

    fun `test cancelling the offset changes nothing`() {
        myFixture.configureByText("notes.md", "> A\n>> 1<caret>. a")
        TestDialogManager.setTestInputDialog { null }
        myFixture.performEditorAction("ChevronLists.OffsetListNumbers")
        assertEquals("> A\n>> 1. a", text)
    }

    fun `test set list start renumbers from the caret`() {
        myFixture.configureByText("notes.md", "> A\n>> 1<caret>. a\n>> 2. b")
        TestDialogManager.setTestInputDialog { "69" }
        myFixture.performEditorAction("ChevronLists.SetListStartNumber")
        assertEquals("> A\n>> 69. a\n>> 70. b", text)
    }

    fun `test set list start inserts a new item off a list`() {
        myFixture.configureByText("notes.md", "> <caret>A\n>> - b")
        TestDialogManager.setTestInputDialog { "7" }
        myFixture.performEditorAction("ChevronLists.SetListStartNumber")
        assertEquals("> A\n>> 7. \n>> - b", text)
        assertEquals(1, myFixture.editor.caretModel.logicalPosition.line)
        assertEquals(6, myFixture.editor.caretModel.logicalPosition.column)
    }

    fun `test wrap item text`() {
        myFixture.configureByText("notes.md", ">> - hello <caret>world")
        myFixture.performEditorAction("ChevronLists.WrapItemText")
        assertEquals(">> - hello\n>>   world", text)
        assertEquals(1, myFixture.editor.caretModel.logicalPosition.line)
        assertEquals(5, myFixture.editor.caretModel.logicalPosition.column)
    }

    fun `test wrap item text needs words on both sides`() {
        myFixture.configureByText("notes.md", ">> - hello<caret>")
        myFixture.performEditorAction("ChevronLists.WrapItemText")
        assertEquals(">> - hello", text)
    }
}
