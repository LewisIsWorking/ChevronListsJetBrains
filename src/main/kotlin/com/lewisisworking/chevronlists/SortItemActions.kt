/**
 * SortItemActions.kt
 * IntelliJ bridge for SortItems.kt: sorts the items of the section at the caret.
 * All decisions are made by the pure computeSortSection; this class only reads
 * the document and rewrites the changed span.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

abstract class ChevronSortItemsAction(
    actionText:             String,
    actionDescription:      String,
    private val descending: Boolean
) : AnAction(actionText, actionDescription, null) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix

        val before = document.text.split("\n")
        val line   = document.getLineNumber(editor.caretModel.offset)
        val after  = computeSortSection(before, line, descending, prefix) ?: return
        val span   = changedSpan(before, after) ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            val start = document.getLineStartOffset(span.first)
            val end   = document.getLineEndOffset(span.last)
            document.replaceString(start, end, after.subList(span.first, span.last + 1).joinToString("\n"))
        }
    }
}

class SortItemsAscendingAction : ChevronSortItemsAction(
    actionText        = "CL: Sort Items A to Z",
    actionDescription = "Sort the items of this section A to Z, keeping nested items with their parent",
    descending        = false
)

class SortItemsDescendingAction : ChevronSortItemsAction(
    actionText        = "CL: Sort Items Z to A",
    actionDescription = "Sort the items of this section Z to A, keeping nested items with their parent",
    descending        = true
)
