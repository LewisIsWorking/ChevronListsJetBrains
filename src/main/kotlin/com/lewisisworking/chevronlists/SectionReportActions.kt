/**
 * SectionReportActions.kt
 * IntelliJ bridge for SectionReports.kt, and the fold-all actions.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.LightVirtualFile
import java.awt.datatransfer.StringSelection

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

class InsertTableOfContentsAction : MarkdownEditorAction(
    "CL: Insert Table of Contents (TOC)", "Insert a section of links to every section in this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = editor.document.text.split("\n")
        val block  = tableOfContents(before)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No sections found to build a table of contents")
        val after = insertSectionBlock(before, editor.caretModel.logicalPosition.line, block)
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }
    }
}

class ShowReadingTimeAction : MarkdownEditorAction(
    "CL: Show Reading Time", "Show the words and reading time of this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val message = readingTimeMessage(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line, listPrefix)
        HintManager.getInstance().showInformationHint(editor, message)
    }
}

class ShowSectionPathAction : MarkdownEditorAction(
    "CL: Show Section Path", "Show where the caret is in its section and how much the section holds"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val message = sectionPathMessage(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line, listPrefix)
        HintManager.getInstance().showInformationHint(editor, message ?: "CL: Not inside a section")
    }
}

class CopyItemAsMarkdownAction : MarkdownEditorAction(
    "CL: Copy Item as Markdown", "Copy the item at the caret as a standard markdown list line"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val hints  = HintManager.getInstance()
        val text   = editor.document.text.split("\n")[editor.caretModel.logicalPosition.line]
        val markdown = itemAsMarkdown(text, listPrefix)
            ?: return hints.showInformationHint(editor, "CL: Place the caret on a chevron item")
        CopyPasteManager.getInstance().setContents(StringSelection(markdown))
        hints.showInformationHint(editor, "CL: Copied as markdown")
    }
}

class ConvertSectionToTableAction : MarkdownEditorAction(
    "CL: Convert Section to Markdown Table", "Open the section's items as a markdown table in a new file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val table   = sectionAsMarkdownTable(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line, listPrefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No items found in this section")
        FileEditorManager.getInstance(project).openFile(LightVirtualFile("Section table.md", table), true)
    }
}

abstract class SetAllFoldsAction(text: String, description: String, private val expanded: Boolean) :
    MarkdownEditorAction(text, description) {
    override fun actionPerformed(e: AnActionEvent) {
        val folding = (e.getData(CommonDataKeys.EDITOR) ?: return).foldingModel
        folding.runBatchFoldingOperation { folding.allFoldRegions.forEach { it.isExpanded = expanded } }
    }
}

class FoldAllSectionsAction : SetAllFoldsAction("CL: Fold All Sections", "Collapse every section in this file", false)

class UnfoldAllSectionsAction : SetAllFoldsAction("CL: Unfold All Sections", "Expand every section in this file", true)
