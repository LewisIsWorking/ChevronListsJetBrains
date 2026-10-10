/**
 * SetTotalsPainter.kt
 * IntelliJ bridge for setSummary in Sets.kt: paints a set's totals in grey
 * after its `#set` heading, the way inline Git blame is painted. Nothing is
 * written to the file. Only heading lines that carry `#set` pay for the count.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.EditorLinePainter
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SetTotalsPainter : EditorLinePainter() {
    override fun getLineExtensions(project: Project, file: VirtualFile, lineNumber: Int): Collection<LineExtensionInfo>? {
        if (file.extension != "md") return null
        val document = FileDocumentManager.getInstance().getCachedDocument(file) ?: return null
        if (lineNumber >= document.lineCount) return null
        val start = document.getLineStartOffset(lineNumber)
        val line  = document.charsSequence.subSequence(start, document.getLineEndOffset(lineNumber))
        if (!line.startsWith("#") || !line.contains("#set", ignoreCase = true)) return null
        val prefix  = ChevronListsSettings.getInstance().state.listPrefix
        val summary = setSummary(document.text.split("\n"), lineNumber, prefix) ?: return null
        val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT)
        return listOf(LineExtensionInfo("    $summary", attributes))
    }
}
