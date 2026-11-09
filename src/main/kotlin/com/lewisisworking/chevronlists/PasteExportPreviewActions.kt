/**
 * PasteExportPreviewActions.kt
 * Thin actions over PasteExportPreview.kt, and the Text Transform palette.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.LightVirtualFile
import java.awt.datatransfer.StringSelection
import java.time.LocalDate

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

private fun askOne(project: Project, title: String, prompt: String, choices: List<String>): String? =
    Messages.showInputDialog(project, prompt, title, null, choices.first(),
        oneOfValidator(choices.toSet()) { listOf(it.trim()) })?.trim()

class SmartPasteAction : MarkdownEditorAction(
    "CL: Smart Paste", "Paste the clipboard as chevron items, keeping a numbered list numbered"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val items  = smartPasted(clipboardText(), documentLines(editor), caretLine(editor), listPrefix)
        insertBelowCaret(e, editor, items)
        if (items.isNotEmpty()) tell(editor, "CL: Pasted ${plural(items.size, "item")}")
    }
}

class CopySectionAsAction : MarkdownEditorAction(
    "CL: Copy Section As...", "Copy this section as Markdown, plain text, JSON, CSV or HTML"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val format  = askOne(project, "Copy Section As", "Format (${COPY_FORMATS.joinToString(", ")}):", COPY_FORMATS) ?: return
        val text    = sectionCopiedAs(format, documentLines(editor), caretLine(editor), listPrefix)
            ?: return tell(editor, "CL: Nothing to copy at cursor")
        CopyPasteManager.getInstance().setContents(StringSelection(text))
        tell(editor, "CL: Copied as $format")
    }
}

class ExportToObsidianAction : MarkdownEditorAction(
    "CL: Export to Obsidian", "Open this file as Obsidian-compatible markdown in a new document"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val content = fileAsObsidian(documentLines(editor), listPrefix, LocalDate.now())
        tell(editor, "CL: Obsidian export ready - save the file to use in Obsidian")
        FileEditorManager.getInstance(project).openFile(LightVirtualFile("Obsidian export.md", content), true)
    }
}

class PreviewItemAction : MarkdownEditorAction(
    "CL: Preview Item", "Show every marker of this item in readable form"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val preview = itemPreview(documentLines(editor)[caretLine(editor)], listPrefix, LocalDate.now())
            ?: return tell(editor, "CL: Place cursor on a chevron item to preview it")
        tell(editor, "<html>${preview.joinToString("<br>") { escapeHtml(it) }}</html>")
    }
}

private val TRANSFORMS = listOf("Bold", "Italic", "Underline", "Mono", "Strikethrough", "UPPERCASE", "lowercase", "Title Case")

private fun styleAction(name: String): AnAction? = when (name) {
    "Bold"          -> BoldTextAction()
    "Italic"        -> ItalicTextAction()
    "Underline"     -> UnderlineTextAction()
    "Mono"          -> MonoTextAction()
    "Strikethrough" -> StrikethroughTextAction()
    else            -> null
}

private fun cased(name: String, text: String) = when (name) {
    "UPPERCASE" -> text.uppercase()
    "lowercase" -> text.lowercase()
    else        -> toTitleCase(text)
}

/** The selection, or else the word at the caret; null when there is neither */
private fun selectionOrWord(editor: Editor): TextRange? {
    val selection = editor.selectionModel
    if (selection.hasSelection()) return TextRange(selection.selectionStart, selection.selectionEnd)
    val document = editor.document
    val line     = caretLine(editor)
    val start    = document.getLineStartOffset(line)
    val text     = document.getText(TextRange(start, document.getLineEndOffset(line)))
    return wordRangeAt(text, editor.caretModel.logicalPosition.column)?.let { TextRange(start + it.first, start + it.last + 1) }
}

class TextTransformAction : MarkdownEditorAction(
    "CL: Text Transform", "Choose a text style or case for the selection or word at the cursor"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val name    = askOne(project, "Text Transform", "Transform (${TRANSFORMS.joinToString(", ")}):", TRANSFORMS) ?: return
        styleAction(name)?.let { return it.actionPerformed(e) }
        val range = selectionOrWord(editor) ?: return tell(editor, "CL: Select text or place the cursor on a word")
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.replaceString(range.startOffset, range.endOffset, cased(name, editor.document.getText(range)))
        }
    }
}
