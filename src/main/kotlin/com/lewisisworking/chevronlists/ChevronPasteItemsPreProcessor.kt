/**
 * ChevronPasteItemsPreProcessor.kt
 * IntelliJ bridge for PasteItems.kt: rewrites a multi-line paste into a chevron
 * item so that each line becomes its own item. All decisions are made by the pure
 * `itemsFromPaste`; this class only reads the editor state.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class ChevronPasteItemsPreProcessor : CopyPastePreProcessor {

    override fun preprocessOnCopy(file: PsiFile, startOffsets: IntArray, endOffsets: IntArray, text: String): String? = null

    override fun preprocessOnPaste(project: Project, file: PsiFile, editor: Editor, text: String, rawText: RawText?): String {
        val settings = ChevronListsSettings.getInstance().state
        if (!settings.pasteLinesAsItems) return text
        if (!file.name.endsWith(".md")) return text
        if (editor.caretModel.caretCount != 1) return text

        val document = editor.document
        val selection = editor.selectionModel
        val start = if (selection.hasSelection()) selection.selectionStart else editor.caretModel.offset
        val end   = if (selection.hasSelection()) selection.selectionEnd   else start
        val line  = document.getLineNumber(start)
        if (document.getLineNumber(end) != line) return text

        val lineStart = document.getLineStartOffset(line)
        val lineText  = document.getText(TextRange(lineStart, document.getLineEndOffset(line)))
        return itemsFromPaste(lineText, start - lineStart, text, settings.listPrefix) ?: text
    }
}
