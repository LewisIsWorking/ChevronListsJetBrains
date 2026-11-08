/**
 * ItemDateActions.kt
 * IntelliJ bridge for ItemDates.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import java.time.LocalDate

class SetDueDateAction : MarkdownEditorAction(
    "CL: Set Due Date", "Set or replace the @YYYY-MM-DD due date on the item at the caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val line     = editor.caretModel.logicalPosition.line
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix
        val text     = document.text.split("\n")[line]
        if (transformItemLine(text, prefix) { it } == null) {
            return HintManager.getInstance().showInformationHint(editor, "CL: Place the caret on a chevron item")
        }
        val validator = object : InputValidatorEx {
            override fun getErrorText(input: String) =
                if (parseNaturalDate(input, LocalDate.now()) != null) null else "Try 2026-12-31, friday, +7 or today"
            override fun checkInput(input: String) = getErrorText(input) == null
            override fun canClose(input: String) = checkInput(input)
        }
        val input = Messages.showInputDialog(
            e.project, "ISO (2026-12-31), weekday (friday), relative (+7), today, tomorrow, next week or next month:",
            "Set Due Date", null, "", validator
        ) ?: return
        val date = parseNaturalDate(input, LocalDate.now()) ?: return
        val updated = transformItemLine(text, prefix) { setItemDueDate(it, date) } ?: return
        WriteCommandAction.runWriteCommandAction(e.project) {
            document.replaceString(document.getLineStartOffset(line), document.getLineEndOffset(line), updated)
        }
    }
}

class InsertDateStampAction : MarkdownEditorAction(
    "CL: Insert Date Stamp", "Insert today's date as @YYYY-MM-DD at every caret, replacing any selection"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val stamp  = "@${LocalDate.now()}"
        WriteCommandAction.runWriteCommandAction(e.project) {
            editor.caretModel.runForEachCaret { caret ->
                if (caret.hasSelection()) {
                    val start = caret.selectionStart
                    editor.document.replaceString(start, caret.selectionEnd, stamp)
                    caret.removeSelection()
                    caret.moveToOffset(start + stamp.length)
                } else {
                    EditorModificationUtil.insertStringAtCaret(editor, stamp)
                }
            }
        }
    }
}

class FixNumberingAction : MarkdownEditorAction(
    "CL: Fix Numbering", "Renumber every list in the document from 1; nested lists are counted separately"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val before   = document.text.split("\n")
        val after    = fixNumbering(before)
        val span     = changedSpan(before, after)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Numbering is already in sequence")
        WriteCommandAction.runWriteCommandAction(e.project) {
            document.replaceString(
                document.getLineStartOffset(span.first), document.getLineEndOffset(span.last),
                after.subList(span.first, span.last + 1).joinToString("\n")
            )
        }
    }
}

class SelectSectionItemsAction : MarkdownEditorAction(
    "CL: Select Section Items", "Put a caret on every item of the caret section, selecting each one"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix
        val items    = sectionItemLines(document.text.split("\n"), editor.caretModel.logicalPosition.line, prefix)
        if (items.isEmpty()) return HintManager.getInstance().showInformationHint(editor, "CL: No items in this section")
        editor.caretModel.setCaretsAndSelections(items.map { line ->
            val start = editor.offsetToLogicalPosition(document.getLineStartOffset(line))
            val end   = editor.offsetToLogicalPosition(document.getLineEndOffset(line))
            CaretState(end, start, end)
        })
    }
}
