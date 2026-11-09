/**
 * GroupsSnippetsActions.kt
 * Thin actions over GroupsSnippets.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import java.time.LocalDate

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

class GroupSectionsAction : MarkdownEditorAction(
    "CL: Group Sections", "Start a named group of sections above the section at the cursor"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val header = headerAtOrAbove(documentLines(editor), editor.caretModel.logicalPosition.line)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No section header found at cursor")
        val name = Messages.showInputDialog(e.project, "Name for this section group:", "Group Sections", null)
        if (name.isNullOrBlank()) return
        WriteCommandAction.runWriteCommandAction(e.project) {
            editor.document.insertString(editor.document.getLineStartOffset(header), groupDivider(name) + "\n")
        }
    }
}

class FilterGroupsAction : MarkdownEditorAction(
    "CL: Filter Groups", "List the section groups of this file and jump to one"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Jump to a section group", groupEntries(documentLines(editor)),
            "CL: No section groups found (use CL: Group Sections or type >> -- Name)")
    }
}

class InsertItemSnippetAction : MarkdownEditorAction(
    "CL: Insert Item Snippet", "Add a ready-made item below the cursor"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val snippets = itemSnippets(LocalDate.now().toString())
        val names    = snippets.map { it.first }
        val name     = Messages.showInputDialog(project, "Snippet (${names.joinToString(", ")}):", "Insert Item Snippet", null,
            names.first(), oneOfValidator(names.toSet()) { listOf(it.trim()) })?.trim() ?: return
        val template = snippets.firstOrNull { it.first == name }?.second ?: return
        val snippet  = expandItemSnippet(template)
        val lead     = ">> $listPrefix "
        WriteCommandAction.runWriteCommandAction(project) {
            val document = editor.document
            val lineEnd  = document.getLineEndOffset(editor.caretModel.logicalPosition.line)
            document.insertString(lineEnd, "\n$lead${snippet.text}")
            val textStart = lineEnd + 1 + lead.length
            editor.selectionModel.setSelection(textStart + snippet.selectionStart, textStart + snippet.selectionEnd)
            editor.caretModel.moveToOffset(textStart + snippet.selectionEnd)
        }
    }
}
