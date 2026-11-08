/**
 * HeaderItemConvertActions.kt
 * IntelliJ bridge for HeaderItemConvert.kt: rewrites one line, an item into a
 * header or a header into an item.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document

private fun replaceLine(e: AnActionEvent, document: Document, line: Int, text: String) {
    WriteCommandAction.runWriteCommandAction(e.project) {
        document.replaceString(document.getLineStartOffset(line), document.getLineEndOffset(line), text)
    }
}

class PromoteItemToHeaderAction : MarkdownEditorAction(
    "CL: Promote Item to Header",
    "Turn the item at the caret into a section header; the items after it start a new section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val line   = editor.caretModel.logicalPosition.line
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val header = itemAsHeader(editor.document.text.split("\n"), line, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Put the caret on an item to promote it")
        replaceLine(e, editor.document, line, header)
    }
}

class DemoteHeaderToItemAction : MarkdownEditorAction(
    "CL: Demote Header to Item",
    "Turn the caret section's header into an item of the section above"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val (line, item) = headerAsItem(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No section above to demote into")
        replaceLine(e, editor.document, line, item)
    }
}
