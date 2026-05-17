/**
 * ToggleDoneAction.kt
 * AnAction that toggles the done state of the current chevron item, or all
 * chevron items in the current selection. Lines that are not chevron items
 * are skipped silently. Disabled when not in a .md file.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange

class ToggleDoneAction : AnAction("CL: Toggle Done", "Toggle the [x] done state of the current chevron item", null) {

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
            // Iterate descending so earlier line offsets stay valid as we edit
            for (line in endLine downTo startLine) {
                val lineStart = document.getLineStartOffset(line)
                val lineEnd   = document.getLineEndOffset(line)
                val lineText  = document.getText(TextRange(lineStart, lineEnd))
                val newLine   = computeToggleDone(lineText, prefix) ?: continue
                document.replaceString(lineStart, lineEnd, newLine)
            }
        }
    }
}