/**
 * SectionSnapshotActionsTest.kt
 * Runs the archive and snapshot actions in a headless IDE. Show Archive opens a
 * popup, so it is run with nothing to list.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SectionSnapshotActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun answer(text: String) = TestDialogManager.setTestInputDialog { text }

    private fun openDoc(name: String) = String(
        FileEditorManager.getInstance(project).openFiles.first { it.name == name }.contentsToByteArray()
    )

    fun `test every action is registered`() {
        for (id in listOf("ArchiveSection", "ShowArchive", "SnapshotSection", "RestoreSnapshot", "ListSnapshots", "SnapshotItem", "DiffItemWithSnapshot")) {
            assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
        }
    }

    fun `test archive section moves it to a new archive`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n\n> B\n>> - b\n")
        myFixture.performEditorAction("ChevronLists.ArchiveSection")
        assertEquals("> B\n>> - b\n\n> Archive\n> A\n>> - a\n", myFixture.editor.document.text)
    }

    fun `test show archive with no archive changes nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n")
        myFixture.performEditorAction("ChevronLists.ShowArchive")
        assertEquals("> A\n", myFixture.editor.document.text)
    }

    fun `test a snapshot is restored over later edits`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n")
        answer("first")
        myFixture.performEditorAction("ChevronLists.SnapshotSection")

        val document = myFixture.editor.document
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            document.setText("> A\n>> - changed\n")
        }
        answer("first")
        myFixture.performEditorAction("ChevronLists.RestoreSnapshot")
        assertEquals("> A\n>> - a\n", document.text)

        // Last: this opens another editor, which then has the focus
        myFixture.performEditorAction("ChevronLists.ListSnapshots")
        assertTrue(openDoc("Snapshots.md").contains("- first ("))
    }

    fun `test restoring with no snapshots changes nothing`() {
        myFixture.configureByText("other.md", "> A<caret>\n>> - a\n")
        myFixture.performEditorAction("ChevronLists.RestoreSnapshot")
        assertEquals("> A\n>> - a\n", myFixture.editor.document.text)
    }

    fun `test an item diff compares with the snapshot`() {
        myFixture.configureByText("notes.md", "> A\n>> - buy red<caret> milk\n")
        myFixture.performEditorAction("ChevronLists.SnapshotItem")
        val document = myFixture.editor.document
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            val red = document.text.indexOf("red")
            document.replaceString(red, red + 3, "blue")
        }
        myFixture.performEditorAction("ChevronLists.DiffItemWithSnapshot")
        assertTrue(openDoc("Item snapshot diff.md").endsWith("- red\n+ blue"))
    }
}
