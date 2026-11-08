/**
 * SectionInsertActionsTest.kt
 * Runs CL: New Section and CL: Clone Section in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SectionInsertActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    fun `test new section goes after the caret section with the caret on its first item`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a1\n\n> B")
        TestDialogManager.setTestInputDialog { " New " }
        myFixture.performEditorAction("ChevronLists.NewSection")
        myFixture.checkResult("> A\n>> - a1\n\n> New\n>> - <caret>\n\n> B")
    }

    fun `test new section at the end of a file`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a1")
        TestDialogManager.setTestInputDialog { "New" }
        myFixture.performEditorAction("ChevronLists.NewSection")
        myFixture.checkResult("> A\n>> - a1\n\n> New\n>> - <caret>")
    }

    fun `test cancelling the name changes nothing`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a1")
        TestDialogManager.setTestInputDialog { null }
        myFixture.performEditorAction("ChevronLists.NewSection")
        assertEquals("> A\n>> - a1", myFixture.editor.document.text)
    }

    fun `test clone inserts a copy and moves the caret to its header`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a1\n\n> B")
        myFixture.performEditorAction("ChevronLists.CloneSection")
        myFixture.checkResult("> A\n>> - a1\n\n<caret>> A (copy)\n>> - a1\n\n> B")
    }
}
