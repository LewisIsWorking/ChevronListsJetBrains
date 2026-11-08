/**
 * DuplicateItemAction.kt
 * IntelliJ bridge for the actions that copy an item: applies the pure
 * DuplicateEdit a subclass computes and moves the caret onto the copy.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.LogicalPosition

abstract class ChevronItemCopyAction(text: String, description: String) : MarkdownEditorAction(text, description) {

    /** The copy for the item on line [index], or null when there is nothing to copy */
    protected abstract fun compute(lines: List<String>, index: Int, listPrefix: String): DuplicateEdit?

    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix
        val caret    = editor.caretModel.currentCaret
        val column   = caret.logicalPosition.column

        val edit = compute(document.text.split("\n"), document.getLineNumber(caret.offset), prefix) ?: return

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

class DuplicateItemAction : ChevronItemCopyAction(
    "CL: Duplicate Item",
    "Copy this item, with anything nested under it, below itself; a numbered copy takes the next number"
) {
    override fun compute(lines: List<String>, index: Int, listPrefix: String) =
        computeDuplicateItem(lines, index, listPrefix)
}
