/**
 * SectionCompareActions.kt
 * Thin actions over SectionCompare.kt. The word cloud is a list with bars here,
 * and VS Code's section quick picks are validated input boxes.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.LightVirtualFile
import java.awt.datatransfer.DataFlavor

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private const val NO_SECTION = "CL: No section found at the caret"

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun AnActionEvent.openMarkdown(name: String, content: String) {
    FileEditorManager.getInstance(project ?: return).openFile(LightVirtualFile(name, content), true)
}

private fun caretHeader(editor: Editor) = headerAtOrAbove(documentLines(editor), editor.caretModel.logicalPosition.line)

class ShowWordFrequencyAction : MarkdownEditorAction(
    "CL: Show Word Frequency", "Show the ten most used words of this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val header = caretHeader(editor) ?: return tell(editor, NO_SECTION)
        tell(editor, sectionWordsMessage(documentLines(editor), header, listPrefix))
    }
}

class ShowWordCloudAction : MarkdownEditorAction(
    "CL: Show Word Cloud", "List this section's most used words with a bar each"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val header = caretHeader(editor) ?: return tell(editor, NO_SECTION)
        showReport(editor, "Word Cloud", wordCloudEntries(documentLines(editor), header, listPrefix), "CL: Not enough words to build a cloud")
    }
}

class ShowTagHeatmapAction : MarkdownEditorAction(
    "CL: Show Tag Heatmap", "Rank the sections by how many tags they hold"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Tag Heatmap", tagHeatmap(documentLines(editor), listPrefix), "CL: No sections found")
    }
}

class ShowCompletionHeatmapAction : MarkdownEditorAction(
    "CL: Show Completion Heatmap", "Rank the sections by how much of their checkboxes are done"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Completion Heatmap", completionHeatmap(documentLines(editor), listPrefix), "CL: No sections found")
    }
}

class CompareTwoSectionsAsTableAction : MarkdownEditorAction(
    "CL: Compare Two Sections as Table", "Open two sections' items side by side in a table"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val (a, b) = askTwoSections(e, editor, "Compare Two Sections as Table") ?: return
        e.openMarkdown("Section table comparison.md", sectionsSideBySide(documentLines(editor), a, b, listPrefix))
    }
}

class DiffTwoSectionsAction : MarkdownEditorAction(
    "CL: Diff Two Sections", "Open a line-by-line diff of two sections' items"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val (a, b) = askTwoSections(e, editor, "Diff Two Sections") ?: return
        e.openMarkdown("Section diff.md", sectionsDiff(documentLines(editor), a, b, listPrefix))
    }
}

class CompareSectionToClipboardAction : MarkdownEditorAction(
    "CL: Compare Section to Clipboard", "Open which items are only in this section or only on the clipboard"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val header = caretHeader(editor) ?: return tell(editor, NO_SECTION)
        val text   = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor).orEmpty()
        val report = sectionAgainstClipboard(documentLines(editor), header, listPrefix, text)
            ?: return tell(editor, "CL: Clipboard is empty")
        e.openMarkdown("Section vs clipboard.md", report)
    }
}
