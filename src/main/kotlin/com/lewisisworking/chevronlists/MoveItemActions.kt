/**
 * MoveItemActions.kt
 * IntelliJ bridge for MoveItems.kt: moves the item at the caret up or down
 * among its siblings. All decisions are made by the pure computeMoveItem; this
 * class only reads the document, writes the changed span and moves the caret.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.LogicalPosition

abstract class ChevronMoveItemAction(
    actionText:        String,
    actionDescription: String,
    private val up:    Boolean
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
        val caret    = editor.caretModel.currentCaret
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix

        val before = document.text.split("\n")
        val line   = document.getLineNumber(caret.offset)
        val result = computeMoveItem(before, line, up, prefix) ?: return
        val span   = changedSpan(before, result.lines) ?: return
        val column = caret.logicalPosition.column

        WriteCommandAction.runWriteCommandAction(project) {
            val start = document.getLineStartOffset(span.first)
            val end   = document.getLineEndOffset(span.last)
            document.replaceString(start, end, result.lines.subList(span.first, span.last + 1).joinToString("\n"))
        }
        caret.moveToLogicalPosition(LogicalPosition(result.newIndex, column))
    }
}

class MoveItemUpAction : ChevronMoveItemAction(
    actionText        = "CL: Move Item Up",
    actionDescription = "Move this item above the previous item at the same depth, with anything nested under it",
    up                = true
)

class MoveItemDownAction : ChevronMoveItemAction(
    actionText        = "CL: Move Item Down",
    actionDescription = "Move this item below the next item at the same depth, with anything nested under it",
    up                = false
)
