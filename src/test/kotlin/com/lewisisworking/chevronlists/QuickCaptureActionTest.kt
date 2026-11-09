/**
 * QuickCaptureActionTest.kt
 * Runs Quick Capture in a headless IDE. It asks for the section, then the text.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuickCaptureActionTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun text() = myFixture.editor.document.text

    private fun capture(section: String?, item: String?) {
        var asked = 0
        TestDialogManager.setTestInputDialog { if (asked++ == 0) section else item }
        myFixture.performEditorAction("ChevronLists.QuickCapture")
    }

    fun `test the item goes at the end of the chosen section`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n\n> B\n>> - b\n")
        capture("A", "  new item ")
        assertEquals("> A\n>> - a\n>> - new item\n\n> B\n>> - b\n", text())
    }

    fun `test the section need not be the one at the cursor`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n\n> B\n>> - b\n")
        capture("B", "new")
        assertEquals("> A\n>> - a\n\n> B\n>> - b\n>> - new\n", text())
    }

    fun `test a section with no items takes the item below its header`() {
        myFixture.configureByText("notes.md", "> A<caret>\n> B\n")
        capture("A", "new")
        assertEquals("> A\n>> - new\n> B\n", text())
    }

    fun `test blank text adds nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n")
        capture("A", "   ")
        assertEquals("> A\n>> - a\n", text())
    }

    fun `test cancelling the section adds nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n")
        capture(null, "new")
        assertEquals("> A\n>> - a\n", text())
    }

    fun `test a file with no sections changes nothing`() {
        myFixture.configureByText("notes.md", "prose<caret>\n")
        capture("A", "new")
        assertEquals("prose\n", text())
    }
}
