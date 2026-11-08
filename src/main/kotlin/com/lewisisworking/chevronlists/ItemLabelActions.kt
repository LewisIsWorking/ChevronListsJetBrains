/**
 * ItemLabelActions.kt
 * IntelliJ bridge for ItemLabels.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.StringSelection

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private const val RATING_PROMPT   = "Stars from 1 to 5, or 0 to remove the rating:"
private const val RATING_ERROR    = "Enter a number from 0 to 5"
private const val PRIORITY_PROMPT = "Priority: 1 for !, 2 for !!, 3 for !!!"
private const val PRIORITY_ERROR  = "Enter 1, 2 or 3"

private fun askNumber(e: AnActionEvent, prompt: String, title: String, error: String, allowed: IntRange): Int? =
    Messages.showInputDialog(e.project, prompt, title, null, "", numberValidator(error) { it in allowed })?.trim()?.toIntOrNull()

private fun isOnItem(editor: Editor, lines: List<String>): Boolean {
    if (itemContent(lines[editor.caretModel.logicalPosition.line], listPrefix) != null) return true
    HintManager.getInstance().showInformationHint(editor, "CL: Place the caret on a chevron item")
    return false
}

private fun changeCaretItem(e: AnActionEvent, editor: Editor, lines: List<String>, change: (String) -> String) {
    val index = editor.caretModel.logicalPosition.line
    val after = lines.toMutableList().also { it[index] = transformItemLine(it[index], listPrefix, change)!! }
    WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, lines, after) }
}

class SetItemRatingAction : MarkdownEditorAction(
    "CL: Set Item Rating", "Rate the item at the caret from 1 to 5 stars"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = editor.document.text.split("\n")
        if (!isOnItem(editor, lines)) return
        val rating = askNumber(e, RATING_PROMPT, "Set Item Rating", RATING_ERROR, 0..5) ?: return
        changeCaretItem(e, editor, lines) { setItemRating(it, rating) }
    }
}

class BulkSetRatingAction : MarkdownEditorAction(
    "CL: Bulk Set Rating", "Rate every item in the section from 1 to 5 stars"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val rating = askNumber(e, RATING_PROMPT, "Bulk Set Rating", RATING_ERROR, 0..5) ?: return
        changeSectionItems(e, editor) { setItemRating(it, rating) }
    }
}

class SetItemPriorityAction : MarkdownEditorAction(
    "CL: Set Item Priority", "Set the priority of the item at the caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = editor.document.text.split("\n")
        if (!isOnItem(editor, lines)) return
        val level = askNumber(e, PRIORITY_PROMPT, "Set Item Priority", PRIORITY_ERROR, 1..3) ?: return
        changeCaretItem(e, editor, lines) { setItemPriority(it, level) }
    }
}

class BulkSetPriorityAction : MarkdownEditorAction(
    "CL: Bulk Set Priority", "Set the priority of every item in the section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val level  = askNumber(e, PRIORITY_PROMPT, "Bulk Set Priority", PRIORITY_ERROR, 1..3) ?: return
        changeSectionItems(e, editor) { setItemPriority(it, level) }
    }
}

class CloneItemToSectionAction : MarkdownEditorAction(
    "CL: Clone Item to Section", "Copy the item at the caret to the end of a section you name"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = editor.document.text.split("\n")
        if (!isOnItem(editor, lines)) return
        val names = lines.mapNotNull { parseHeader(it)?.content?.trim() }
        if (names.isEmpty()) return HintManager.getInstance().showInformationHint(editor, "CL: No sections found")
        val validator = object : InputValidatorEx {
            override fun getErrorText(input: String) = if (findSectionNamed(lines, input) != null) null else "No section has that name"
            override fun checkInput(input: String) = getErrorText(input) == null
            override fun canClose(input: String) = checkInput(input)
        }
        val message = "Clone the item to which section?\n${names.joinToString(", ")}"
        val name    = Messages.showInputDialog(e.project, message, "Clone Item to Section", null, "", validator) ?: return
        val header  = findSectionNamed(lines, name) ?: return
        val edit    = cloneItemToSection(lines, editor.caretModel.logicalPosition.line, listPrefix, header) ?: return
        val after   = lines.take(edit.afterLine + 1) + edit.insert + lines.drop(edit.afterLine + 1)
        WriteCommandAction.runWriteCommandAction(e.project) {
            replaceChangedLines(editor.document, lines, after)
            editor.caretModel.moveToLogicalPosition(LogicalPosition(edit.afterLine + 1, 0))
            editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        }
    }
}

class ExtractUrlsFromSectionAction : MarkdownEditorAction(
    "CL: Extract URLs from Section", "Copy every URL in the section's items to the clipboard"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val hints  = HintManager.getInstance()
        val urls   = sectionUrls(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line, listPrefix)
            ?: return hints.showInformationHint(editor, "CL: No section found at caret")
        if (urls.isEmpty()) return hints.showInformationHint(editor, "CL: No URLs found in this section")
        CopyPasteManager.getInstance().setContents(StringSelection(urls.joinToString("\n")))
        hints.showInformationHint(editor, "CL: Copied ${urls.size} URL${if (urls.size == 1) "" else "s"}")
    }
}
