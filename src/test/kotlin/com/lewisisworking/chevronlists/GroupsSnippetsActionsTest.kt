/**
 * GroupsSnippetsActionsTest.kt
 * Runs Group Sections, Filter Groups and Insert Item Snippet in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GroupsSnippetsActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun text() = myFixture.editor.document.text

    fun `test a divider goes above the section at the cursor`() {
        myFixture.configureByText("notes.md", "> A\n> B\n>> - b<caret>\n")
        TestDialogManager.setTestInputDialog { " Act One " }
        myFixture.performEditorAction("ChevronLists.GroupSections")
        assertEquals("> A\n>> -- Act One\n> B\n>> - b\n", text())
    }

    fun `test outside a section no divider is added`() {
        myFixture.configureByText("notes.md", "prose<caret>\n> A\n")
        TestDialogManager.setTestInputDialog { "Act" }
        myFixture.performEditorAction("ChevronLists.GroupSections")
        assertEquals("prose\n> A\n", text())
    }

    fun `test no groups changes nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n")
        myFixture.performEditorAction("ChevronLists.FilterGroups")
        assertEquals("> A\n", text())
    }

    fun `test a snippet is added below the cursor with its first placeholder selected`() {
        myFixture.configureByText("notes.md", "> A<caret>\n> B\n")
        TestDialogManager.setTestInputDialog { "Starred Note" }
        myFixture.performEditorAction("ChevronLists.InsertItemSnippet")
        assertEquals("> A\n>> - * note\n> B\n", text())
        assertEquals("note", myFixture.editor.selectionModel.selectedText)
    }
}
