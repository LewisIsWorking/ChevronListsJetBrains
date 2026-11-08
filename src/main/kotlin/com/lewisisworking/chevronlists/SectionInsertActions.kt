/**
 * SectionInsertActions.kt
 * IntelliJ bridge for SectionInserts.kt: adds a new section, or a "(copy)" of the
 * caret section, and moves the caret into it.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.ui.Messages

private fun applyInsert(e: AnActionEvent, document: Document, insert: SectionBlockInsert) {
    WriteCommandAction.runWriteCommandAction(e.project) {
        val text = insert.lines.joinToString("\n")
        if (insert.beforeLine < document.lineCount) {
            document.insertString(document.getLineStartOffset(insert.beforeLine), text + "\n")
        } else {
            document.insertString(document.textLength, "\n" + text)
        }
    }
}

class NewSectionAction : MarkdownEditorAction(
    "CL: New Section",
    "Add a new section with an empty first item"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val name = Messages.showInputDialog(project, "New section name:", "New Section", null)?.trim()
        if (name.isNullOrEmpty()) return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val insert = newSectionInsert(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line, name, prefix)
        applyInsert(e, editor.document, insert)
        editor.caretModel.moveToLogicalPosition(LogicalPosition(insert.headerLine + 1, ">> $prefix ".length))
    }
}

class CloneSectionAction : MarkdownEditorAction(
    "CL: Clone Section",
    "Insert a copy of the caret section below it, named \"(copy)\", and go to it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val insert = cloneSectionInsert(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No section found at the caret")
        applyInsert(e, editor.document, insert)
        editor.caretModel.moveToLogicalPosition(LogicalPosition(insert.headerLine, 0))
    }
}
