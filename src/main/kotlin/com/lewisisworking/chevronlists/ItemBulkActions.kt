/**
 * ItemBulkActions.kt
 * IntelliJ bridge for ItemBulkEdits.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.DataFlavor
import java.time.LocalDate

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

internal fun changeSectionItems(e: AnActionEvent, editor: Editor, change: (String) -> String) {
    val before = editor.document.text.split("\n")
    val line   = editor.caretModel.logicalPosition.line
    val hints  = HintManager.getInstance()
    if (headerAtOrAbove(before, line) == null) return hints.showInformationHint(editor, "CL: No section found at caret")
    val prefix = listPrefix
    val after  = applyToSection(before, line) { mapSectionItems(it, prefix, change) }
        ?: return hints.showInformationHint(editor, "CL: Nothing to change in this section")
    WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }
}

private val dateValidator = object : InputValidatorEx {
    override fun getErrorText(input: String) =
        if (parseNaturalDate(input, LocalDate.now()) != null) null else "Try 2026-12-31, friday, +7 or today"
    override fun checkInput(input: String) = getErrorText(input) == null
    override fun canClose(input: String) = checkInput(input)
}

private fun askDate(e: AnActionEvent, title: String): String? {
    val input = Messages.showInputDialog(e.project, "ISO (2026-12-31), weekday (friday), relative (+7), today or tomorrow:", title, null, "", dateValidator)
        ?: return null
    return parseNaturalDate(input, LocalDate.now())
}

class StampItemAction : MarkdownEditorAction(
    "CL: Stamp Item with Creation Date", "Add @created:today to the item at the caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val line     = editor.caretModel.logicalPosition.line
        val text     = document.text.split("\n")[line]
        val prefix   = listPrefix
        val hints    = HintManager.getInstance()
        val content  = itemContent(text, prefix)
            ?: return hints.showInformationHint(editor, "CL: Place the caret on a chevron item to stamp it")
        if (hasCreatedStamp(content)) return hints.showInformationHint(editor, "CL: Item already has a creation date")
        val updated = transformItemLine(text, prefix) { stampCreated(it, LocalDate.now().toString()) } ?: return
        WriteCommandAction.runWriteCommandAction(e.project) {
            document.replaceString(document.getLineStartOffset(line), document.getLineEndOffset(line), updated)
        }
    }
}

class StampAllItemsAction : MarkdownEditorAction(
    "CL: Stamp All Items", "Add @created:today to every item in the section without one"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val today  = LocalDate.now().toString()
        changeSectionItems(e, editor) { if (hasCreatedStamp(it)) it else stampCreated(it, today) }
    }
}

class BulkTagItemsAction : MarkdownEditorAction(
    "CL: Bulk Tag Items", "Add a tag to every item in the section"
) {
    private val validator = object : InputValidatorEx {
        override fun getErrorText(input: String) =
            if (Regex("""\w[\w-]*""").matches(input.trim().trimStart('#'))) null else "Use letters, numbers, hyphens and underscores"
        override fun checkInput(input: String) = getErrorText(input) == null
        override fun canClose(input: String) = checkInput(input)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val input  = Messages.showInputDialog(e.project, "Tag to add to all items:", "Bulk Tag Items", null, "", validator) ?: return
        val tag    = input.trim().trimStart('#')
        if (tag.isNotEmpty()) changeSectionItems(e, editor) { addItemTag(it, tag) }
    }
}

class BulkSetDueDateAction : MarkdownEditorAction(
    "CL: Bulk Set Due Date", "Set the due date on every item in the section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val date   = askDate(e, "Bulk Set Due Date") ?: return
        changeSectionItems(e, editor) { setItemDueDate(it, date) }
    }
}

class SetExpiryOnAllItemsAction : MarkdownEditorAction(
    "CL: Set Expiry on All Items", "Set an @expires: date on every item in the section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val date   = askDate(e, "Set Expiry on All Items") ?: return
        changeSectionItems(e, editor) { setItemExpiry(it, date) }
    }
}

internal fun clipboardText(): String = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor) ?: ""

internal fun insertBelowCaret(e: AnActionEvent, editor: Editor, items: List<String>) {
    if (items.isEmpty()) return HintManager.getInstance().showInformationHint(editor, "CL: Clipboard is empty")
    val document = editor.document
    val line     = editor.caretModel.logicalPosition.line
    WriteCommandAction.runWriteCommandAction(e.project) {
        document.insertString(document.getLineEndOffset(line), "\n" + items.joinToString("\n"))
    }
}

class PasteAsBulletsAction : MarkdownEditorAction(
    "CL: Paste Clipboard as Bullet Items", "Paste each copied line as a bullet item below the caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        insertBelowCaret(e, editor, pastedBullets(clipboardText(), listPrefix))
    }
}

class PasteAsNumberedAction : MarkdownEditorAction(
    "CL: Paste Clipboard as Numbered Items", "Paste each copied line as a numbered item below the caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = editor.document.text.split("\n")
        insertBelowCaret(e, editor, pastedNumbered(clipboardText(), lines, editor.caretModel.logicalPosition.line))
    }
}

class PasteAsSectionAction : MarkdownEditorAction(
    "CL: Paste Clipboard as Section", "Paste the copied lines as a new section, the first line naming it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val block  = pastedSection(clipboardText(), listPrefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Clipboard is empty")
        val before = editor.document.text.split("\n")
        val after  = insertSectionBlock(before, editor.caretModel.logicalPosition.line, block)
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }
    }
}
