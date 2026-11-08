/**
 * MergeSplitActions.kt
 * IntelliJ bridge for MergeSplitSections.kt: merges the caret section with the
 * one below it, or splits it at the caret item.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages

class MergeSectionBelowAction : MarkdownEditorAction(
    "CL: Merge Section Below",
    "Join the next section's items onto the caret section, removing its header"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val range    = mergeSectionBelowRange(document.text.split("\n"), editor.caretModel.logicalPosition.line)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No section below to merge with")
        WriteCommandAction.runWriteCommandAction(project) {
            // Take the line break after the range, or before it when the header is the last line
            if (range.last + 1 < document.lineCount) {
                document.deleteString(document.getLineStartOffset(range.first), document.getLineStartOffset(range.last + 1))
            } else {
                document.deleteString(document.getLineEndOffset(range.first - 1), document.textLength)
            }
        }
    }
}

class SplitSectionHereAction : MarkdownEditorAction(
    "CL: Split Section Here",
    "Start a new section at the caret item"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val line     = editor.caretModel.logicalPosition.line
        val offered  = splitSectionName(document.text.split("\n"), line)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Put the caret on an item line to split the section there")
        val name = Messages.showInputDialog(project, "Name for the new section:", "Split Section Here", null, offered, null)
            ?.trim()
        if (name.isNullOrEmpty()) return
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.getLineStartOffset(line), "> $name\n")
        }
    }
}
