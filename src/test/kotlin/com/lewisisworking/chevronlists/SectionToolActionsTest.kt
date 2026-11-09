/**
 * SectionToolActionsTest.kt
 * Runs the section tool actions in a headless IDE. Actions that open a list or a
 * save dialog are run on files with nothing to list, as neither can show here.
 */
package com.lewisisworking.chevronlists

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Color

class SectionToolActionsTest : BasePlatformTestCase() {

    private val ids = listOf(
        "SetSectionColour", "ShowSectionGrowth", "SearchItems", "FilterSections",
        "CompareSectionStatistics", "ExportStatisticsCsv", "ExportStatisticsJson",
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

    fun `test every action is registered`() {
        for (id in ids) assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
    }

    fun `test lists and exports with nothing to show leave the text alone`() {
        for (id in listOf("ShowSectionGrowth", "SearchItems", "FilterSections", "ExportStatisticsCsv", "ExportStatisticsJson")) {
            assertEquals("text", runAction(id, "<caret>text"))
        }
    }

    fun `test set colour tags the section header`() {
        answer("Blue")
        assertEquals("> A [colour:blue]\n>> - a", runAction("SetSectionColour", "> A\n>> - <caret>a"))
    }

    fun `test none clears the colour`() {
        answer("none")
        assertEquals("> A", runAction("SetSectionColour", "> A [colour:red]<caret>"))
    }

    fun `test set colour needs a section`() {
        assertEquals("text", runAction("SetSectionColour", "<caret>text"))
    }

    fun `test compare opens a table`() {
        answer("A", "b")
        runAction("CompareSectionStatistics", "> A<caret>\n>> - a\n> B")
        val file = FileEditorManager.getInstance(project).openFiles.first { it.name == "Section comparison.md" }
        assertTrue(String(file.contentsToByteArray()).contains("| Items | 1 | 0 |"))
    }

    fun `test compare needs two sections`() {
        assertEquals("> A", runAction("CompareSectionStatistics", "> A<caret>"))
    }

    fun `test a section colour paints the header`() {
        myFixture.configureByText("notes.md", "> A [colour:red]")
        val red = Color.decode(ColourLabel.RED.hex)
        val painted = myFixture.doHighlighting(HighlightSeverity.INFORMATION).filter { it.forcedTextAttributes?.foregroundColor == red }
        assertTrue(painted.toString(), painted.any { it.startOffset == 2 && it.endOffset == 16 })
    }
}
