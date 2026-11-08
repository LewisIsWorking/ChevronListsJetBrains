/**
 * ListNumberingActions.kt
 * IntelliJ bridge for ListNumbering.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages

internal fun numberValidator(error: String, valid: (Int) -> Boolean) = object : InputValidatorEx {
    override fun getErrorText(input: String) = if (input.trim().toIntOrNull()?.let(valid) == true) null else error
    override fun checkInput(input: String) = getErrorText(input) == null
    override fun canClose(input: String) = checkInput(input)
}

private fun plural(count: Int) = if (count == 1) "$count item" else "$count items"

class RebaseListFromHereAction : MarkdownEditorAction(
    "CL: Rebase List From Here", "Renumber from the caret down, leaving items above untouched"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val before   = document.text.split("\n")
        val hints    = HintManager.getInstance()
        val edit     = rebaseListFrom(before, editor.caretModel.logicalPosition.line)
            ?: return hints.showInformationHint(editor, "CL: Place the caret on a numbered item to rebase from here")
        if (edit.changed == 0) return hints.showInformationHint(editor, "CL: All items already in sequence")
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(document, before, edit.lines) }
        hints.showInformationHint(editor, "CL: Rebased ${plural(edit.changed)}")
    }
}

class OffsetListNumbersAction : MarkdownEditorAction(
    "CL: Offset List Numbers", "Add a fixed amount to every number in the section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val before   = document.text.split("\n")
        val line     = editor.caretModel.logicalPosition.line
        val hints    = HintManager.getInstance()
        if (offsetListNumbers(before, line, 1) == null) return hints.showInformationHint(editor, "CL: No section found at caret")
        val input = Messages.showInputDialog(
            e.project, "Offset all numbers by (use negative to subtract):", "Offset List Numbers", null, "",
            numberValidator("Enter a non-zero integer") { it != 0 }
        ) ?: return
        val offset = input.trim().toIntOrNull() ?: return
        val result = offsetListNumbers(before, line, offset) ?: return
        if (result.tooLow > 0) {
            return hints.showInformationHint(editor, "CL: Offsetting by $offset would take ${plural(result.tooLow)} below 1 - nothing changed")
        }
        if (result.changed == 0) return hints.showInformationHint(editor, "CL: No numbered items in this section")
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(document, before, result.lines) }
        hints.showInformationHint(editor, "CL: Offset ${plural(result.changed)} by ${if (offset > 0) "+" else ""}$offset")
    }
}

class SetListStartNumberAction : MarkdownEditorAction(
    "CL: Set List Start Number", "Renumber this list from a chosen number, or start a new numbered item"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val before   = document.text.split("\n")
        val line     = editor.caretModel.logicalPosition.line
        val existing = parseNumbered(before[line])
        val message  = if (existing != null) "Renumber from here - current value is ${existing.num}. New start number:" else "Start numbered list at:"
        val input = Messages.showInputDialog(
            e.project, message, "Set List Start Number", null, (existing?.num ?: 1).toString(),
            numberValidator("Enter a positive integer") { it > 0 }
        ) ?: return
        val start = input.trim().toIntOrNull() ?: return
        WriteCommandAction.runWriteCommandAction(e.project) {
            if (existing != null) {
                replaceChangedLines(document, before, setListStart(before, line, start) ?: before)
            } else {
                val item = ">> $start. "
                document.insertString(document.getLineEndOffset(line), "\n$item")
                editor.caretModel.moveToLogicalPosition(LogicalPosition(line + 1, item.length))
            }
        }
    }
}

class WrapItemTextAction : MarkdownEditorAction(
    "CL: Wrap Item Text", "Move the words after the caret onto an indented continuation line"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val position = editor.caretModel.logicalPosition
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix
        val wrap     = wrapItemAt(document.text.split("\n")[position.line], position.column, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Place the caret inside an item's words to wrap it")
        WriteCommandAction.runWriteCommandAction(e.project) {
            document.replaceString(
                document.getLineStartOffset(position.line), document.getLineEndOffset(position.line),
                "${wrap.first}\n${wrap.second}"
            )
            editor.caretModel.moveToLogicalPosition(LogicalPosition(position.line + 1, wrap.column))
        }
    }
}
