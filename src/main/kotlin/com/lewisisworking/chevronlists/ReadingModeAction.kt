/**
 * ReadingModeAction.kt
 * Thin action over fileAsHtml in FileExports.kt: renders the current file as
 * the styled standalone HTML page and opens it in the system browser.
 *
 * The JB-native counterpart of the VS Code extension's reading mode
 * (CL: Enter Reading Mode): VS Code shows the page in a live-updating
 * webview beside the editor, while IntelliJ hands the same page to the
 * system browser. Re-invoke after edits to refresh the page.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.io.IOException
import java.nio.file.Files

class ReadingModeAction : MarkdownEditorAction(
    "CL: Reading Mode", "Open this file as a styled reading page in the system browser"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val listPrefix = ChevronListsSettings.getInstance().state.listPrefix
        val lines = documentLines(editor)
        if (lines.none { isHeader(it) || itemContent(it, listPrefix) != null }) {
            HintManager.getInstance().showInformationHint(editor, "CL: Nothing to read")
            return
        }
        val base = FileDocumentManager.getInstance().getFile(editor.document)?.nameWithoutExtension ?: "notes"
        try {
            val page = Files.createTempFile("$base-reading-", ".html")
            Files.writeString(page, fileAsHtml(lines, listPrefix, base))
            page.toFile().deleteOnExit()
            BrowserUtil.browse(page.toUri())
        } catch (failure: IOException) {
            HintManager.getInstance().showInformationHint(editor, "CL: Could not open reading mode: ${failure.message}")
        }
    }
}
