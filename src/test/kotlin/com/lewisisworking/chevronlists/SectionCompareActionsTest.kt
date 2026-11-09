/**
 * SectionCompareActionsTest.kt
 * Runs the section comparison actions in a headless IDE. Actions that open a
 * list are run with nothing to list, as a popup cannot show here.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.StringSelection

class SectionCompareActionsTest : BasePlatformTestCase() {

    private val ids = listOf(
        "ShowWordFrequency", "ShowWordCloud", "ShowTagHeatmap", "ShowCompletionHeatmap",
        "CompareTwoSectionsAsTable", "DiffTwoSections", "CompareSectionToClipboard",
    )

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun answer(vararg answers: String) {
        val queue = ArrayDeque(answers.toList())
        TestDialogManager.setTestInputDialog { queue.removeFirst() }
    }

    private fun runAction(id: String, text: String): String {
        myFixture.configureByText("notes.md", text)
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    private fun opened(name: String) =
        String(FileEditorManager.getInstance(project).openFiles.first { it.name == name }.contentsToByteArray())

    fun `test every action is registered`() {
        for (id in ids) assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
    }

    fun `test actions outside a section leave the text alone`() {
        for (id in ids) assertEquals(id, "text", runAction(id, "<caret>text"))
    }

    fun `test word frequency needs no popup`() {
        assertEquals("> A\n>> - milk", runAction("ShowWordFrequency", "> A<caret>\n>> - milk"))
    }

    fun `test the table opens for two named sections`() {
        answer("A", "B")
        runAction("CompareTwoSectionsAsTable", "> A<caret>\n>> - a\n> B\n>> - b")
        assertTrue(opened("Section table comparison.md").contains("| a | b |"))
    }

    fun `test the diff opens for two named sections`() {
        answer("a", "b")
        runAction("DiffTwoSections", "> A<caret>\n>> - a\n> B\n>> - b")
        assertTrue(opened("Section diff.md").contains("- a\n+ b"))
    }

    fun `test the clipboard comparison opens`() {
        CopyPasteManager.getInstance().setContents(StringSelection(">> - a\nb"))
        runAction("CompareSectionToClipboard", "> A<caret>\n>> - a")
        assertTrue(opened("Section vs clipboard.md").contains("= a"))
    }
}
