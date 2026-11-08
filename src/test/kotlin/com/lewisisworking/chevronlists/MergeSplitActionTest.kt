/**
 * MergeSplitActionTest.kt
 * Runs CL: Merge Section Below and CL: Split Section Here in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MergeSplitActionTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    fun `test merge joins the next section's items onto this one`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a1\n\n> B\n>> - b1\n\n> C")
        myFixture.performEditorAction("ChevronLists.MergeSectionBelow")
        assertEquals("> A\n>> - a1\n>> - b1\n\n> C", myFixture.editor.document.text)
    }

    fun `test merging an empty last section leaves no trailing line break`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a1\n> B")
        myFixture.performEditorAction("ChevronLists.MergeSectionBelow")
        assertEquals("> A\n>> - a1", myFixture.editor.document.text)
    }

    fun `test split inserts a header above the caret item`() {
        myFixture.configureByText("notes.md", "> A\n>> - a1\n>> - <caret>a2")
        TestDialogManager.setTestInputDialog { "Later" }
        myFixture.performEditorAction("ChevronLists.SplitSectionHere")
        assertEquals("> A\n>> - a1\n> Later\n>> - a2", myFixture.editor.document.text)
    }

    fun `test cancelling the split changes nothing`() {
        myFixture.configureByText("notes.md", "> A\n>> - a1\n>> - <caret>a2")
        TestDialogManager.setTestInputDialog { null }
        myFixture.performEditorAction("ChevronLists.SplitSectionHere")
        assertEquals("> A\n>> - a1\n>> - a2", myFixture.editor.document.text)
    }
}
