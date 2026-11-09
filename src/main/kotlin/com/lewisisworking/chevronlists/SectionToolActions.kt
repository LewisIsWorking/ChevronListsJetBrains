/**
 * SectionToolActions.kt
 * Thin actions over SectionTools.kt and the statistics exports. VS Code's quick
 * picks are validated input boxes or searchable lists here.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightVirtualFile
import java.nio.file.Files
import java.time.LocalDate

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private const val NO_COLOUR = "none"

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun sectionValidator(names: List<String>) = object : InputValidatorEx {
    override fun getErrorText(input: String) = if (names.any { it.equals(input.trim(), ignoreCase = true) }) null else "No section has that name"
    override fun checkInput(input: String) = getErrorText(input) == null
    override fun canClose(input: String) = checkInput(input)
}

private fun askSection(e: AnActionEvent, title: String, prompt: String, names: List<String>): String? =
    Messages.showInputDialog(e.project, prompt, title, null, names.first(), sectionValidator(names))

/** Asks for two different sections by name and returns their header lines, or null if there are not two or the user cancels */
internal fun askTwoSections(e: AnActionEvent, editor: Editor, title: String): Pair<Int, Int>? {
    val lines = documentLines(editor)
    val names = sectionNames(lines)
    if (names.size < 2) { tell(editor, "CL: Need at least 2 sections to compare"); return null }
    val first  = askSection(e, title, "First section:", names) ?: return null
    val others = names.filterNot { it.equals(first.trim(), ignoreCase = true) }
    if (others.isEmpty()) { tell(editor, "CL: Need at least 2 sections to compare"); return null }
    val second = askSection(e, title, "Second section:", others) ?: return null
    return (sectionNamed(lines, first) ?: return null) to (sectionNamed(lines, second) ?: return null)
}

class SetSectionColourAction : MarkdownEditorAction(
    "CL: Set Section Colour", "Colour this section's header, or clear its colour"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = documentLines(editor)
        val header = headerAtOrAbove(before, editor.caretModel.logicalPosition.line)
            ?: return tell(editor, "CL: No section header found at the caret")
        val names  = ColourLabel.values().map { it.displayName }
        val answer = Messages.showInputDialog(
            e.project, "Colour (${names.joinToString(", ")} or $NO_COLOUR):", "Set Section Colour", null,
            sectionColourOf(before[header])?.displayName ?: names.first(),
            oneOfValidator((names + NO_COLOUR).toSet()) { listOf(it.trim().lowercase()) },
        )?.trim()?.lowercase() ?: return
        val colour = ColourLabel.values().firstOrNull { it.displayName == answer }
        if (colour == null && answer != NO_COLOUR) return
        val after  = before.toMutableList().also { it[header] = withSectionColour(before[header], colour) }
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }
    }
}

class ShowSectionGrowthAction : MarkdownEditorAction(
    "CL: Show Section Growth", "Chart how many items each section holds, fullest first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Section Growth", sectionGrowth(documentLines(editor), listPrefix), "CL: No sections found")
    }
}

class SearchItemsAction : MarkdownEditorAction(
    "CL: Search Items", "Type to find an item in this file and jump to it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Search Items", itemSearchEntries(documentLines(editor), listPrefix), "CL: No items found in this file")
    }
}

class FilterSectionsAction : MarkdownEditorAction(
    "CL: Filter Sections", "Type to find a section in this file and jump to it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Filter Sections", sectionSearchEntries(documentLines(editor)), "CL: No sections found in this file")
    }
}

class CompareSectionStatisticsAction : MarkdownEditorAction(
    "CL: Compare Section Statistics", "Open a table comparing two sections of this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val (a, b)  = askTwoSections(e, editor, "Compare Section Statistics") ?: return
        val lines   = documentLines(editor)
        FileEditorManager.getInstance(project)
            .openFile(LightVirtualFile("Section comparison.md", compareSections(lines, a, b, listPrefix)), true)
    }
}

abstract class ExportStatisticsAction(
    text: String, description: String, private val extension: String, private val render: (FileStats) -> String,
) : MarkdownEditorAction(text, description) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val stats   = fileStats(documentLines(editor), listPrefix, LocalDate.now().toString())
        if (stats.sections.isEmpty()) return tell(editor, "CL: No sections found")
        val source = FileDocumentManager.getInstance().getFile(editor.document)
        val folder = source?.parent?.takeIf { it.isInLocalFileSystem }
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
            FileSaverDescriptor("Export Statistics", "Choose where to save the statistics", *arrayOf(extension)), project)
        val target = dialog.save(folder, "${source?.nameWithoutExtension ?: "notes"}-stats.$extension") ?: return
        Files.writeString(target.file.toPath(), render(stats))
        tell(editor, "CL: Statistics exported to ${target.file.name}")
    }
}

class ExportStatisticsCsvAction : ExportStatisticsAction(
    "CL: Export Statistics as CSV", "Save each section's item and word counts as a CSV file", "csv", ::fileStatsAsCsv
)

class ExportStatisticsJsonAction : ExportStatisticsAction(
    "CL: Export Statistics as JSON", "Save this file's statistics as a JSON file", "json", ::fileStatsAsJson
)
