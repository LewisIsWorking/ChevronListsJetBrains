/**
 * SectionReportActionsTest.kt
 * Runs the contents, markdown, table and folding actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor

class SectionReportActionsTest : BasePlatformTestCase() {

    private fun act(id: String, initial: String): String {
        myFixture.configureByText("notes.md", initial)
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    fun `test insert table of contents after the section`() = assertEquals(
        "> A\n>> - a\n\n> Table of Contents\n>> - [[A]]",
        act("InsertTableOfContents", "> A\n>> - <caret>a"),
    )

    fun `test reports leave the text alone`() {
        assertEquals("> A\n>> - a", act("ShowReadingTime", "> A\n>> - <caret>a"))
        assertEquals("> A\n>> - a", act("ShowSectionPath", "> A\n>> - <caret>a"))
    }

    fun `test copy item as markdown`() {
        act("CopyItemAsMarkdown", ">> 2. <caret>a #t")
        assertEquals("2. a **#t**", CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)!!)
    }

    fun `test convert section to table opens a new file`() {
        act("ConvertSectionToTable", "> A\n>> - <caret>a")
        assertTrue(FileEditorManager.getInstance(project).openFiles.any { it.name == "Section table.md" })
    }

    fun `test fold and unfold all`() {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a\n> B\n>> - b")
        val folding = myFixture.editor.foldingModel
        folding.runBatchFoldingOperation {
            folding.addFoldRegion(0, 3, "A")
            folding.addFoldRegion(11, 14, "B")
        }
        myFixture.performEditorAction("ChevronLists.FoldAllSections")
        assertTrue(folding.allFoldRegions.none { it.isExpanded })
        myFixture.performEditorAction("ChevronLists.UnfoldAllSections")
        assertTrue(folding.allFoldRegions.all { it.isExpanded })
    }
}
