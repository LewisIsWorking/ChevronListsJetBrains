/**
 * DuplicateItemAction.kt
 * IntelliJ bridge for DuplicateItem.kt: duplicates the item at the caret, with
 * everything nested under it, and moves the caret onto the copy. All decisions
 * are made by the pure computeDuplicateItem.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.LogicalPosition

class DuplicateItemAction : AnAction(
    "CL: Duplicate Item",
    "Copy this item, with anything nested under it, below itself; a numbered copy takes the next number",
    null
) {
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
        val caret    = editor.caretModel.currentCaret
        val column   = caret.logicalPosition.column

        val lines = document.text.split("\n")
        val edit  = computeDuplicateItem(lines, document.getLineNumber(caret.offset), prefix) ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            // Renumber from the bottom up, then insert: every line index still
            // refers to the original document when it is used
            for ((line, text) in edit.renumbered.entries.sortedByDescending { it.key }) {
                document.replaceString(document.getLineStartOffset(line), document.getLineEndOffset(line), text)
            }
            document.insertString(document.getLineEndOffset(edit.afterLine), "\n" + edit.insert.joinToString("\n"))
        }
        caret.moveToLogicalPosition(LogicalPosition(edit.afterLine + 1, column))
    }
}
