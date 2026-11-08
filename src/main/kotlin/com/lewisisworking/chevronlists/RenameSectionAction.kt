/**
 * RenameSectionAction.kt
 * IntelliJ bridge for SectionRename.kt: asks for a new name for the caret
 * section and rewrites its header and links in one undoable edit.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages

class RenameSectionAction : MarkdownEditorAction(
    "CL: Rename Section",
    "Rename the caret section and update every [[link]] to it in the file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val lines    = document.text.split("\n")
        val (header, oldName) = sectionNameAt(lines, editor.caretModel.logicalPosition.line)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No section header found at the caret")
        val newName = Messages.showInputDialog(project, "New section name:", "Rename Section", null, oldName, null)
            ?.trim()
        if (newName.isNullOrEmpty() || newName == oldName) return

        val result = renameSection(lines, header, newName)
        WriteCommandAction.runWriteCommandAction(project, "Rename Section", null, {
            result.lines.forEachIndexed { i, line ->
                if (line != lines[i]) {
                    document.replaceString(document.getLineStartOffset(i), document.getLineEndOffset(i), line)
                }
            }
        })
        val links = result.linkLines
        HintManager.getInstance().showInformationHint(editor,
            "CL: Renamed to \"$newName\"" + if (links > 0) " - updated $links link${if (links == 1) "" else "s"}" else "")
    }
}
