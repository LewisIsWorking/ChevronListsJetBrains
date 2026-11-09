/**
 * FileExportActions.kt
 * Thin actions over FileExports.kt: each asks where to save, then writes the file.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.io.IOException
import java.nio.file.Files

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

abstract class ExportFileAction(
    text: String,
    description: String,
    private val extension: String,
    private val nameSuffix: String = "",
    private val render: (lines: List<String>, prefix: String, fileName: String) -> String,
) : MarkdownEditorAction(text, description) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = documentLines(editor)
        if (lines.none { isHeader(it) || itemContent(it, listPrefix) != null }) return tell(editor, "CL: Nothing to export")
        val source = FileDocumentManager.getInstance().getFile(editor.document)
        val folder = source?.parent?.takeIf { it.isInLocalFileSystem }
        val base   = source?.nameWithoutExtension ?: "notes"
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
            FileSaverDescriptor("Export File", "Choose where to save the export", *arrayOf(extension)), project)
        val target = dialog.save(folder, "$base$nameSuffix.$extension") ?: return
        try {
            Files.writeString(target.file.toPath(), render(lines, listPrefix, base))
            tell(editor, "CL: Exported to ${target.file.name}")
        } catch (failure: IOException) {
            tell(editor, "CL: Could not write ${target.file.name}: ${failure.message}")
        }
    }
}

class ExportFileAsJsonAction : ExportFileAction(
    "CL: Export File as JSON", "Save every section and item of this file as JSON", "json",
    render = { lines, prefix, _ -> fileAsJson(lines, prefix) },
)

class ExportFileAsCsvAction : ExportFileAction(
    "CL: Export File as CSV", "Save every item of this file as a row of a CSV file", "csv",
    render = { lines, prefix, _ -> fileAsCsv(lines, prefix) },
)

class ExportFileAsHtmlAction : ExportFileAction(
    "CL: Export File as HTML", "Save this file as a styled standalone HTML page", "html",
    render = ::fileAsHtml,
)

class ExportFileAsMarkdownDocumentAction : ExportFileAction(
    "CL: Export File as Markdown Document", "Save this file as standard markdown with headings and lists", "md", "-export",
    render = { lines, prefix, _ -> fileAsMarkdown(lines, prefix) },
)

class ExportAllSectionsAsJsonAction : ExportFileAction(
    "CL: Export All Sections as JSON", "Save every section with all of each item's fields as JSON", "json",
    render = ::allSectionsAsJson,
)
