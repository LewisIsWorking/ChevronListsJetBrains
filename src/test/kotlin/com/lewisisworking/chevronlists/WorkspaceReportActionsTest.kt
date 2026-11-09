/**
 * WorkspaceReportActionsTest.kt
 * Runs the workspace actions in a headless IDE. Actions that open a list are run
 * with nothing to list, as a popup cannot show here.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class WorkspaceReportActionsTest : BasePlatformTestCase() {

    private val lists = listOf(
        "TodayView", "FindDeadLinks", "SearchItemsWorkspace", "FilterSectionsWorkspace",
        "FilterByTagWorkspace", "FilterByMentionWorkspace", "FilterByColourLabelWorkspace",
    )

    private fun runAction(id: String, text: String): String {
        myFixture.configureByText("notes.md", text)
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    private fun opened(name: String) =
        String(FileEditorManager.getInstance(project).openFiles.first { it.name == name }.contentsToByteArray())

    fun `test every action is registered`() {
        for (id in lists + listOf("ShowTagReportWorkspace", "ShowWorkspaceStatistics")) {
            assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
        }
    }

    fun `test lists with nothing to show leave the text alone`() {
        for (id in lists) assertEquals(id, "text", runAction(id, "<caret>text"))
    }

    fun `test the scan finds markdown files and skips the rest`() {
        myFixture.addFileToProject("sub/other.md", "> B")
        myFixture.addFileToProject("node_modules/dep/readme.md", "> No")
        myFixture.addFileToProject("code.txt", "> No")
        myFixture.configureByText("notes.md", "> A")
        val names = ReadAction.compute<List<String>, RuntimeException> { readWorkspace(project).files.map { it.name } }
        assertEquals(listOf("notes.md", "other.md"), names.sorted())
    }

    fun `test the scan reads unsaved text`() {
        myFixture.configureByText("notes.md", "<caret>")
        myFixture.type("> Typed")
        val lines = ReadAction.compute<List<List<String>>, RuntimeException> { readWorkspace(project).files.map { it.lines } }
        assertTrue(lines.toString(), lines.contains(listOf("> Typed")))
    }

    fun `test statistics open across files`() {
        myFixture.addFileToProject("other.md", ">> - b")
        runAction("ShowWorkspaceStatistics", ">> - a<caret>")
        val report = opened("Workspace statistics.md")
        assertTrue(report, report.contains("| Files | 2 |"))
        assertTrue(report, report.contains("| Items | 2 |"))
    }

    fun `test the tag report opens`() {
        runAction("ShowTagReportWorkspace", ">> - a #t<caret>")
        assertTrue(opened("Workspace tag report.md").contains("## #t (1 total)"))
    }

    fun `test the tag report needs a tag`() {
        assertEquals("text", runAction("ShowTagReportWorkspace", "<caret>text"))
    }
}
