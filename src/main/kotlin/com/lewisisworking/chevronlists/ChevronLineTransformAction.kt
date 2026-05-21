/**
 * ChevronLineTransformAction.kt
 * Abstract base for actions that compute a new line text from the current line
 * and apply the result via WriteCommandAction. Same shape as
 * ChevronMarkerToggleAction but for general line-level transforms (promote,
 * demote, cycle list type, etc.).
 *
 * Multi-line selection is supported - each line in the selection is processed;
 * non-item lines are skipped silently.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange

abstract class ChevronLineTransformAction(
    actionText:        String,
    actionDescription: String
) : AnAction(actionText, actionDescription, null) {

    /** Subclasses supply the pure transform: given a line and the bullet prefix,
     *  return the new line text, or null if the line should be left unchanged. */
    protected abstract fun transform(line: String, listPrefix: String): String?

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

        val (startLine, endLine) = if (caret.hasSelection()) {
            Pair(document.getLineNumber(caret.selectionStart),
                 document.getLineNumber(caret.selectionEnd))
        } else {
            val n = document.getLineNumber(caret.offset)
            Pair(n, n)
        }

        WriteCommandAction.runWriteCommandAction(project) {
            for (line in endLine downTo startLine) {
                val lineStart = document.getLineStartOffset(line)
                val lineEnd   = document.getLineEndOffset(line)
                val lineText  = document.getText(TextRange(lineStart, lineEnd))
                val newLine   = transform(lineText, prefix) ?: continue
                document.replaceString(lineStart, lineEnd, newLine)
            }
        }
    }
}