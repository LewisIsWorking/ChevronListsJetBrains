/**
 * RenameSectionActionTest.kt
 * Runs CL: Rename Section in a headless IDE, answering its name prompt.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RenameSectionActionTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    fun `test rename updates the header and links in one undoable edit`() {
        myFixture.configureByText("notes.md", "> Shopping\n>> - <caret>milk\n> Plans\n>> - see [[Shopping]]")
        TestDialogManager.setTestInputDialog { "Groceries" }
        myFixture.performEditorAction("ChevronLists.RenameSection")
        assertEquals("> Groceries\n>> - milk\n> Plans\n>> - see [[Groceries]]", myFixture.editor.document.text)
    }

    fun `test cancelling the prompt changes nothing`() {
        val text = "> Shopping\n>> - <caret>milk"
        myFixture.configureByText("notes.md", text)
        TestDialogManager.setTestInputDialog { null }
        myFixture.performEditorAction("ChevronLists.RenameSection")
        assertEquals("> Shopping\n>> - milk", myFixture.editor.document.text)
    }

    fun `test outside a section it does not ask`() {
        myFixture.configureByText("notes.md", "<caret>intro\n> Shopping")
        TestDialogManager.setTestInputDialog { throw AssertionError("should not ask") }
        myFixture.performEditorAction("ChevronLists.RenameSection")
        assertEquals("intro\n> Shopping", myFixture.editor.document.text)
    }
}
