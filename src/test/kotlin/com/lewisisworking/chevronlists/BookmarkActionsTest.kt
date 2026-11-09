/**
 * BookmarkActionsTest.kt
 * Runs Add, Jump to and Remove Bookmark in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BookmarkActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun text() = myFixture.editor.document.text

    fun `test a bookmark goes above the cursor line`() {
        myFixture.configureByText("notes.md", "> A\n>> - it<caret>em\n")
        TestDialogManager.setTestInputDialog { " Key point " }
        myFixture.performEditorAction("ChevronLists.AddBookmark")
        assertEquals("> A\n>> [bookmark:Key point]\n>> - item\n", text())
    }

    fun `test a blank name adds nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n")
        TestDialogManager.setTestInputDialog { "   " }
        myFixture.performEditorAction("ChevronLists.AddBookmark")
        assertEquals("> A\n", text())
    }

    fun `test no bookmarks changes nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n")
        myFixture.performEditorAction("ChevronLists.JumpToBookmark")
        assertEquals("> A\n", text())
    }

    fun `test the bookmark on the cursor line is removed`() {
        myFixture.configureByText("notes.md", "> A\n>> [bookmark:Ke<caret>y]\n>> - item\n")
        myFixture.performEditorAction("ChevronLists.RemoveBookmark")
        assertEquals("> A\n>> - item\n", text())
    }

    fun `test a bookmark on the last line is removed with its line`() {
        myFixture.configureByText("notes.md", "> A\n>> [bookmark:Ke<caret>y]")
        myFixture.performEditorAction("ChevronLists.RemoveBookmark")
        assertEquals("> A", text())
    }

    fun `test a line without a bookmark is left alone`() {
        myFixture.configureByText("notes.md", "> A\n>> - it<caret>em\n")
        myFixture.performEditorAction("ChevronLists.RemoveBookmark")
        assertEquals("> A\n>> - item\n", text())
    }
}
