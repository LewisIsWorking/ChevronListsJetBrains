/**
 * ItemEditActions.kt
 * IntelliJ bridge for ItemEdits.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.TextRange

internal fun replaceChangedLines(document: Document, before: List<String>, after: List<String>) {
    // Merging and splitting change the line count, so the two ends are matched separately
    if (before == after) return
    val shared = minOf(before.size, after.size)
    val first = (0 until shared).firstOrNull { before[it] != after[it] } ?: shared
    if (first == before.size) {
        // Lines added after the last line: there is no old line to replace
        document.insertString(document.textLength, after.subList(first, after.size).joinToString("\n", prefix = "\n"))
        return
    }
    if (first == after.size) {
        // Only the end of the document went
        document.deleteString(document.getLineEndOffset(first - 1), document.textLength)
        return
    }
    var lastBefore = before.lastIndex
    var lastAfter  = after.lastIndex
    while (lastBefore > first && lastAfter > first && before[lastBefore] == after[lastAfter]) {
        lastBefore--
        lastAfter--
    }
    document.replaceString(
        document.getLineStartOffset(first), document.getLineEndOffset(lastBefore),
        after.subList(first, lastAfter + 1).joinToString("\n")
    )
}

class MergeItemWithNextAction : MarkdownEditorAction(
    "CL: Merge Item with Next", "Join the item at the caret with the item below it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val before   = document.text.split("\n")
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix
        val position = editor.caretModel.logicalPosition
        val after    = mergeItemWithNext(before, position.line, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Place the caret on an item with an item below it")
        WriteCommandAction.runWriteCommandAction(e.project) {
            replaceChangedLines(document, before, after)
            editor.caretModel.moveToLogicalPosition(position)
        }
    }
}

class SplitItemAtCursorAction : MarkdownEditorAction(
    "CL: Split Item at Cursor", "Split the item at the caret into two items"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val before   = document.text.split("\n")
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix
        val position = editor.caretModel.logicalPosition
        val split    = splitItemAt(before, position.line, position.column, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Place the caret inside an item's words to split it")
        WriteCommandAction.runWriteCommandAction(e.project) {
            replaceChangedLines(document, before, split.lines)
            editor.caretModel.moveToLogicalPosition(LogicalPosition(position.line + 1, split.column))
        }
    }
}

abstract class SortAllSectionsAction(text: String, description: String, private val ascending: Boolean) :
    MarkdownEditorAction(text, description) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val before   = document.text.split("\n")
        val after    = sortAllSections(before, ascending)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Need at least 2 sections to sort")
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(document, before, after) }
    }
}

class SortAllSectionsAscendingAction : SortAllSectionsAction(
    "CL: Sort All Sections A to Z", "Sort every section by name, A to Z", true
)

class SortAllSectionsDescendingAction : SortAllSectionsAction(
    "CL: Sort All Sections Z to A", "Sort every section by name, Z to A", false
)

abstract class TextStyleAction(text: String, description: String, private val style: (String) -> String) :
    MarkdownEditorAction(text, description) {

    private fun targetRange(editor: Editor): IntRange? {
        val caret = editor.caretModel.currentCaret
        if (caret.hasSelection()) return caret.selectionStart until caret.selectionEnd
        val document = editor.document
        val line     = caret.logicalPosition.line
        val start    = document.getLineStartOffset(line)
        val text     = document.getText(TextRange(start, document.getLineEndOffset(line)))
        val word     = wordRangeAt(text, caret.logicalPosition.column) ?: return null
        return word.first + start..word.last + start
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val range    = targetRange(editor) ?: return
        val text     = document.charsSequence.subSequence(range.first, range.last + 1).toString()
        if (text.isBlank()) return
        WriteCommandAction.runWriteCommandAction(e.project) {
            document.replaceString(range.first, range.last + 1, style(text))
        }
    }
}

class BoldTextAction : TextStyleAction("CL: Bold Text", "Toggle bold on the selection or word at the caret", { toggleWrap(it, "**") })

class ItalicTextAction : TextStyleAction("CL: Italic Text", "Toggle italic on the selection or word at the caret", { toggleWrap(it, "_") })

class MonoTextAction : TextStyleAction("CL: Mono Text", "Toggle monospace on the selection or word at the caret", { toggleWrap(it, "`") })

class StrikethroughTextAction : TextStyleAction("CL: Strikethrough Text", "Toggle strikethrough on the selection or word at the caret", { toggleWrap(it, "~~") })

class UnderlineTextAction : TextStyleAction("CL: Underline Text", "Toggle a Unicode underline on the selection or word at the caret", ::toggleUnderline)
