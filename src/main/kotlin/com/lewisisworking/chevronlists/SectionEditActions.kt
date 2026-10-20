/**
 * SectionEditActions.kt
 * IntelliJ bridge for SectionEdits.kt: duplicates or deletes the caret section.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

class DuplicateSectionAction : MarkdownEditorAction(
    "CL: Duplicate Section",
    "Insert a copy of the caret section below it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val insert   = duplicateSection(document.text.split("\n"), editor.caretModel.logicalPosition.line) ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.getLineEndOffset(insert.afterLine), "\n" + insert.lines.joinToString("\n"))
        }
    }
}

class DeleteSectionAction : MarkdownEditorAction(
    "CL: Delete Section",
    "Delete the caret section, its header and everything under it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val range    = deleteSectionRange(document.text.split("\n"), editor.caretModel.logicalPosition.line) ?: return
        // Take the line break after the range, or before it at the end of the file
        val start = when {
            range.last + 1 < document.lineCount -> document.getLineStartOffset(range.first)
            range.first > 0                     -> document.getLineEndOffset(range.first - 1)
            else                                -> 0
        }
        val end = if (range.last + 1 < document.lineCount) document.getLineStartOffset(range.last + 1)
                  else document.textLength
        WriteCommandAction.runWriteCommandAction(project) {
            document.deleteString(start, end)
        }
        editor.caretModel.moveToOffset(start)
    }
}
