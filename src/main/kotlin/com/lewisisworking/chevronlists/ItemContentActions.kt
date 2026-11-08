/**
 * ItemContentActions.kt
 * IntelliJ bridge for ItemContentEdits.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.ui.Messages
import kotlin.math.abs

private const val MAX_SHIFT_DAYS = 1_000_000

private fun Document.caretLineText(editor: Editor) = text.split("\n")[editor.caretModel.logicalPosition.line]

private fun replaceCaretLine(e: AnActionEvent, editor: Editor, updated: String) {
    val line = editor.caretModel.logicalPosition.line
    WriteCommandAction.runWriteCommandAction(e.project) {
        editor.document.replaceString(editor.document.getLineStartOffset(line), editor.document.getLineEndOffset(line), updated)
    }
}

private fun plural(count: Int, noun: String) = if (count == 1) "$count $noun" else "$count ${noun}s"

class EditItemContentAction : MarkdownEditorAction(
    "CL: Edit Item Content", "Edit the item's words, keeping its markers and metadata"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix  = ChevronListsSettings.getInstance().state.listPrefix
        val line    = editor.document.caretLineText(editor)
        val content = itemContent(line, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Place the caret on a chevron item to edit it")
        val plain = stripItemMetadata(content)
        val input = Messages.showInputDialog(e.project, "Edit item content:", "Edit Item Content", null, plain, null) ?: return
        if (input == plain || input.isBlank()) return
        replaceCaretLine(e, editor, replaceItemWords(line, prefix, input) ?: return)
    }
}

class AddQuickNoteAction : MarkdownEditorAction(
    "CL: Add Quick Note to Item", "Add a // comment to the item, replacing any it has"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val line   = editor.document.caretLineText(editor)
        if (itemContent(line, prefix) == null) {
            return HintManager.getInstance().showInformationHint(editor, "CL: Place the caret on a chevron item")
        }
        val note = Messages.showInputDialog(e.project, "Add a quick note (appended as a // comment):", "Add Quick Note", null, "", null)
        if (note.isNullOrBlank()) return
        replaceCaretLine(e, editor, addItemNote(line, prefix, note) ?: return)
    }
}

class ConvertItemToSectionLinkAction : MarkdownEditorAction(
    "CL: Convert Item to Section Link", "Replace the item with a [[link]] to the section sharing its name"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val lines  = editor.document.text.split("\n")
        val linked = itemAsSectionLink(lines, editor.caretModel.logicalPosition.line, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Place the caret on an item named after a section in this file")
        replaceCaretLine(e, editor, linked)
    }
}

class ReplaceInSectionAction : MarkdownEditorAction(
    "CL: Replace in Section", "Find and replace within the words of this section's items"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val hints    = HintManager.getInstance()
        val find = Messages.showInputDialog(e.project, "Find text in section items:", "Replace in Section", null, "", null)
        if (find.isNullOrBlank()) return
        val replacement = Messages.showInputDialog(e.project, "Replace \"$find\" with:", "Replace in Section", null, "", null) ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val before = document.text.split("\n")
        val edit   = replaceInSection(before, editor.caretModel.logicalPosition.line, prefix, find, replacement)
            ?: return hints.showInformationHint(editor, "CL: No section found at caret")
        if (edit.changed == 0) return hints.showInformationHint(editor, "CL: No occurrences of \"$find\" found in section")
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(document, before, edit.lines) }
        hints.showInformationHint(editor, "CL: Replaced ${plural(edit.changed, "occurrence")} in section")
    }
}

class ShiftAllDueDatesAction : MarkdownEditorAction(
    "CL: Shift All Due Dates", "Move the due dates of this section's items by a number of days"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val hints    = HintManager.getInstance()
        val input = Messages.showInputDialog(
            e.project, "Shift all due dates by how many days? (use negative to move back):", "Shift All Due Dates", null, "",
            numberValidator("Enter a non-zero integer") { it != 0 && abs(it) <= MAX_SHIFT_DAYS }
        ) ?: return
        val days   = input.trim().toLongOrNull() ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val before = document.text.split("\n")
        val edit   = shiftSectionDueDates(before, editor.caretModel.logicalPosition.line, prefix, days)
            ?: return hints.showInformationHint(editor, "CL: No section found at caret")
        if (edit.changed == 0) return hints.showInformationHint(editor, "CL: No due dates found in this section")
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(document, before, edit.lines) }
        val word = if (abs(days) == 1L) "day" else "days"
        hints.showInformationHint(editor, "CL: Shifted ${plural(edit.changed, "date")} by ${if (days > 0) "+" else ""}$days $word")
    }
}

class InsertGroupDividerAction : MarkdownEditorAction(
    "CL: Insert Group Divider", "Insert a >> -- Name divider below the caret line"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val name = Messages.showInputDialog(e.project, "Group divider name:", "Insert Group Divider", null, "", null)
        if (name.isNullOrBlank()) return
        val line    = editor.caretModel.logicalPosition.line
        val divider = ">> -- ${name.trim()}"
        WriteCommandAction.runWriteCommandAction(e.project) {
            document.insertString(document.getLineEndOffset(line), "\n$divider")
            editor.caretModel.moveToLogicalPosition(
                if (line + 2 < document.lineCount) LogicalPosition(line + 2, 0) else LogicalPosition(line + 1, divider.length)
            )
        }
    }
}
