/**
 * SectionSummaryActions.kt
 * IntelliJ bridge for SectionSummaries.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private const val NO_SECTION = "CL: Not inside a section"
private const val PRIORITY_PROMPT = "Priority: 0 for none, 1 for !, 2 for !!, 3 for !!!"
private const val PRIORITY_ERROR  = "Enter 0, 1, 2 or 3"

private class ReportRenderer : SimpleListCellRenderer<ReportEntry>() {
    override fun customize(list: JList<out ReportEntry>, value: ReportEntry?, index: Int, selected: Boolean, hasFocus: Boolean) {
        text = value?.label ?: ""
    }
}

private fun hint(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

internal fun showReport(editor: Editor, title: String, entries: List<ReportEntry>, whenEmpty: String) {
    if (entries.isEmpty()) {
        hint(editor, whenEmpty)
        return
    }
    JBPopupFactory.getInstance()
        .createPopupChooserBuilder(entries)
        .setTitle(title)
        .setRenderer(ReportRenderer())
        .setNamerForFiltering { it.label }
        .setItemChosenCallback { entry ->
            editor.caretModel.moveToLogicalPosition(LogicalPosition(entry.line, 0))
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
        .createPopup()
        .showInBestPositionFor(editor)
}

internal fun documentLines(editor: Editor) = editor.document.text.split("\n")

private fun caretLines(editor: Editor) = editor.caretModel.allCarets.map { it.logicalPosition.line }

private fun showSectionMessage(editor: Editor, message: (List<String>, Int, String) -> String?) {
    hint(editor, message(documentLines(editor), editor.caretModel.logicalPosition.line, listPrefix) ?: NO_SECTION)
}

private fun editAtCarets(e: AnActionEvent, editor: Editor, change: (List<String>) -> List<String>) {
    val before = documentLines(editor)
    val after  = change(before)
    WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }
}

class ShowWordCountAction : MarkdownEditorAction(
    "CL: Show Word Count", "List the word count of every section in this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Word Count", wordCountReport(documentLines(editor), listPrefix), "CL: No sections found in this file")
    }
}

class ShowNestingSummaryAction : MarkdownEditorAction(
    "CL: Show Nesting Summary", "Show how many items this section has at each depth"
) {
    override fun actionPerformed(e: AnActionEvent) {
        showSectionMessage(e.getData(CommonDataKeys.EDITOR) ?: return, ::nestingSummary)
    }
}

class ShowPrioritySummaryAction : MarkdownEditorAction(
    "CL: Show Priority Summary", "List every item with a priority, most urgent first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(
            editor, "Priority Summary", priorityReport(documentLines(editor), listPrefix),
            "CL: No priority items found (add !, !!, or !!! to items)"
        )
    }
}

class ShowSectionTimeEstimateAction : MarkdownEditorAction(
    "CL: Show Section Time Estimate", "Add up the time estimates in this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        showSectionMessage(e.getData(CommonDataKeys.EDITOR) ?: return, ::sectionEstimateMessage)
    }
}

class CountWordFrequencyAction : MarkdownEditorAction(
    "CL: Count Word Frequency", "Show the most used words in this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        showSectionMessage(e.getData(CommonDataKeys.EDITOR) ?: return, ::wordFrequencyMessage)
    }
}

class ToggleDoneAllCursorsAction : MarkdownEditorAction(
    "CL: Toggle Done (All Cursors)", "Toggle the done state of the item at every caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        editAtCarets(e, editor) { toggleDoneAtLines(it, caretLines(editor), listPrefix) }
    }
}

class SetPriorityAllCursorsAction : MarkdownEditorAction(
    "CL: Set Priority (All Cursors)", "Set the same priority on the item at every caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val level = Messages.showInputDialog(
            e.project, PRIORITY_PROMPT, "Set Priority (All Cursors)", null, "",
            numberValidator(PRIORITY_ERROR) { it in 0..3 }
        )?.trim()?.toIntOrNull() ?: return
        editAtCarets(e, editor) { setPriorityAtLines(it, caretLines(editor), listPrefix, level) }
    }
}
