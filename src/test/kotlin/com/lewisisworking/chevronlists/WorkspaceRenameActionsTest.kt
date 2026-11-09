/**
 * WorkspaceRenameActionsTest.kt
 * Runs the workspace-wide rename actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class WorkspaceRenameActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun textOf(file: VirtualFile) = FileDocumentManager.getInstance().getDocument(file)!!.text

    fun `test a tag is renamed across files`() {
        val other = myFixture.addFileToProject("other.md", "> B\n>> - b #work\n").virtualFile
        myFixture.configureByText("notes.md", "> A\n>> - a #work<caret>\n")
        TestDialogManager.setTestInputDialog { message -> if ("Tag to rename" in message) "#work" else "job" }
        myFixture.performEditorAction("ChevronLists.RenameTagWorkspace")
        assertEquals("> B\n>> - b #job\n", textOf(other))
        assertEquals("> A\n>> - a #job\n", myFixture.editor.document.text)
    }

    fun `test keeping the same tag name changes nothing`() {
        val other = myFixture.addFileToProject("other.md", "> B\n>> - b #work\n").virtualFile
        myFixture.configureByText("notes.md", "> A<caret>\n")
        TestDialogManager.setTestInputDialog { "work" }
        myFixture.performEditorAction("ChevronLists.RenameTagWorkspace")
        assertEquals("> B\n>> - b #work\n", textOf(other))
    }

    fun `test a section and its links are renamed across files`() {
        val other = myFixture.addFileToProject("other.md", "> Plans\n>> - see [[Plans]]\n").virtualFile
        myFixture.configureByText("notes.md", "> Plans<caret>\n>> - [[Plans]]\n")
        TestDialogManager.setTestInputDialog { "Goals" }
        myFixture.performEditorAction("ChevronLists.RenameSectionWorkspace")
        assertEquals("> Plans\n>> - see [[Goals]]\n", textOf(other))
        assertEquals("> Goals\n>> - [[Goals]]\n", myFixture.editor.document.text)
    }

    fun `test outside a section nothing is renamed`() {
        myFixture.configureByText("notes.md", "prose<caret>\n> Plans\n")
        TestDialogManager.setTestInputDialog { "Goals" }
        myFixture.performEditorAction("ChevronLists.RenameSectionWorkspace")
        assertEquals("prose\n> Plans\n", myFixture.editor.document.text)
    }
}
