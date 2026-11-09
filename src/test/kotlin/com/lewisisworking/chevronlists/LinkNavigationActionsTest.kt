/**
 * LinkNavigationActionsTest.kt
 * Runs the link, search and copy actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor

class LinkNavigationActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun caretLine() = myFixture.editor.caretModel.logicalPosition.line

    fun `test the cursor goes to the linked section`() {
        myFixture.configureByText("notes.md", ">> - see [[Go<caret>als]]\n> Plans\n> Goals\n")
        myFixture.performEditorAction("ChevronLists.GoToLinkedSection")
        assertEquals(2, caretLine())
    }

    fun `test a link to a missing section leaves the cursor`() {
        myFixture.configureByText("notes.md", ">> - see [[Gon<caret>e]]\n> Plans\n")
        myFixture.performEditorAction("ChevronLists.GoToLinkedSection")
        assertEquals(0, caretLine())
    }

    fun `test a file link opens the file at its section`() {
        myFixture.addFileToProject("other.md", "> First\n> Wanted\n")
        myFixture.configureByText("notes.md", ">> - [[file:other.md#wan<caret>ted]]\n")
        myFixture.performEditorAction("ChevronLists.GoToLinkedFile")
        val opened = FileEditorManager.getInstance(project).selectedTextEditor!!
        assertEquals("> First\n> Wanted\n", opened.document.text)
        assertEquals(1, opened.caretModel.logicalPosition.line)
    }

    fun `test a file link to a missing file stays put`() {
        myFixture.configureByText("notes.md", ">> - [[file:gone<caret>.md]]\n")
        myFixture.performEditorAction("ChevronLists.GoToLinkedFile")
        assertEquals("notes.md", FileEditorManager.getInstance(project).selectedFiles.single().name)
    }

    fun `test a file section link is inserted at the cursor`() {
        val other = myFixture.addFileToProject("other.md", "> First\n> Wanted\n").virtualFile
        myFixture.configureByText("notes.md", ">> - see <caret>\n")
        val path = other.path.removePrefix("${project.basePath}/")
        TestDialogManager.setTestInputDialog { message -> if ("File" in message) path else "Wanted" }
        myFixture.performEditorAction("ChevronLists.InsertFileSectionLink")
        assertEquals(">> - see [[file:other.md#Wanted]]\n", myFixture.editor.document.text)
    }

    fun `test an item is copied as rich text`() {
        myFixture.configureByText("notes.md", ">>> - [x] do<caret> it @2026-01-02\n")
        myFixture.performEditorAction("ChevronLists.CopyItemAsRichText")
        assertEquals(
            "  ✓ do it (due: 2026-01-02)",
            CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor),
        )
    }

    fun `test a search with no results changes nothing`() {
        myFixture.configureByText("notes.md", "> A\n>> - a<caret>\n")
        TestDialogManager.setTestInputDialog { "zebra" }
        myFixture.performEditorAction("ChevronLists.FindInSections")
        assertEquals(1, caretLine())
    }

    fun `test no dependencies changes nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n")
        myFixture.performEditorAction("ChevronLists.ShowDependencies")
        assertEquals("> A\n", myFixture.editor.document.text)
    }
}
