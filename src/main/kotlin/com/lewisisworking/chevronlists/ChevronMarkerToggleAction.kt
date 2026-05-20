/**
 * ChevronMarkerToggleAction.kt
 * Abstract base for actions that toggle a single emoji/text marker on chevron
 * items (Star, Pin, Flag, Note, ...). Subclasses just supply the marker
 * character and human-readable description.
 *
 * Same behaviour as ToggleDoneAction with multi-line selection support:
 * each line in the selection is processed; non-item lines are skipped silently.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange

abstract class ChevronMarkerToggleAction(
    private val marker:    String,
    actionText:            String,
    actionDescription:     String
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
                val newLine   = computeToggleMarker(lineText, prefix, marker) ?: continue
                document.replaceString(lineStart, lineEnd, newLine)
            }
        }
    }
}