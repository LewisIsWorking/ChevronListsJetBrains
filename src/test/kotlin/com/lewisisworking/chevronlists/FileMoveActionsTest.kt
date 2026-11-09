/**
 * FileMoveActionsTest.kt
 * Runs Move Item to File and Duplicate Section to File in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FileMoveActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun answers(file: String, section: String = "") =
        TestDialogManager.setTestInputDialog { message -> if ("section" in message) section else file }

    private fun textOf(file: VirtualFile) = FileDocumentManager.getInstance().getDocument(file)!!.text

    fun `test an item moves to the other file with its children`() {
        val dest = myFixture.addFileToProject("dest.md", "> Dest\n>> - d\n\n> Other\n").virtualFile
        myFixture.configureByText("notes.md", "> Src\n>> - moving<caret>\n>>> - kid\n>> - stays\n")
        answers("dest.md", "Dest")
        myFixture.performEditorAction("ChevronLists.MoveItemToFile")
        assertEquals("> Dest\n>> - d\n>> - moving\n>>> - kid\n\n> Other\n", textOf(dest))
        assertEquals("> Src\n>> - stays\n", myFixture.editor.document.text)
    }

    fun `test a line that is not an item moves nothing`() {
        val dest = myFixture.addFileToProject("dest.md", "> Dest\n").virtualFile
        myFixture.configureByText("notes.md", "> Src<caret>\n")
        answers("dest.md", "Dest")
        myFixture.performEditorAction("ChevronLists.MoveItemToFile")
        assertEquals("> Dest\n", textOf(dest))
        assertEquals("> Src\n", myFixture.editor.document.text)
    }

    fun `test a file with no sections receives nothing and the item stays`() {
        val dest = myFixture.addFileToProject("dest.md", "just prose\n").virtualFile
        myFixture.configureByText("notes.md", "> Src\n>> - moving<caret>\n")
        answers("dest.md", "Dest")
        myFixture.performEditorAction("ChevronLists.MoveItemToFile")
        assertEquals("just prose\n", textOf(dest))
        assertEquals("> Src\n>> - moving\n", myFixture.editor.document.text)
    }

    fun `test a section is copied to the end of the other file`() {
        val dest = myFixture.addFileToProject("dest.md", "> Dest\n").virtualFile
        myFixture.configureByText("notes.md", "> Src<caret>\n>> - a\n\n> Next\n")
        answers("dest.md")
        myFixture.performEditorAction("ChevronLists.DuplicateSectionToFile")
        assertEquals("> Dest\n\n> Src\n>> - a\n", textOf(dest))
        assertEquals("> Src\n>> - a\n\n> Next\n", myFixture.editor.document.text)
    }

    fun `test with no other file nothing is asked or changed`() {
        myFixture.configureByText("notes.md", "> Src<caret>\n")
        answers("nowhere.md")
        myFixture.performEditorAction("ChevronLists.DuplicateSectionToFile")
        assertEquals("> Src\n", myFixture.editor.document.text)
    }
}
