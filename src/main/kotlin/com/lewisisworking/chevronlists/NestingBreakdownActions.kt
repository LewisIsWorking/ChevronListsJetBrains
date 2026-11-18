/**
 * NestingBreakdownActions.kt
 * IntelliJ bridge for nestingBreakdown in NestingBreakdown.kt: shows how many
 * items sit at each chevron depth in the caret section. Ported from the VS
 * Code extension's CL: Show Nesting Breakdown.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

class ShowNestingBreakdownAction : MarkdownEditorAction(
    "CL: Show Nesting Breakdown",
    "Show how many items are at each chevron depth in the caret section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = editor.document.text.split("\n")
        val header = sectionHeaderAt(lines, editor.caretModel.logicalPosition.line)
        if (header < 0) {
            HintManager.getInstance().showInformationHint(editor, "CL: No section found at the caret")
            return
        }
        val counts = nestingBreakdown(lines, header, ChevronListsSettings.getInstance().state.listPrefix)
        val text   = if (counts.isEmpty()) "CL: \"${parseHeader(lines[header])!!.content.trim()}\" - no items found"
                     else nestingBreakdownText(parseHeader(lines[header])!!.content.trim(), counts)
        Messages.showInfoMessage(e.project, text, "Nesting Breakdown")
    }
}
