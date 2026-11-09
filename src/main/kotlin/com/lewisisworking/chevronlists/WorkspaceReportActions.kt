/**
 * WorkspaceReportActions.kt
 * Thin actions over WorkspaceReports.kt. Each reads the project's markdown files,
 * including unsaved edits, then shows a list that jumps to the file or opens a report.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.SimpleListCellRenderer
import java.time.LocalDate
import javax.swing.JList

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

/** The project's markdown files, as text and by the path each [ScannedFile] carries */
internal class Workspace(val files: List<ScannedFile>, val virtualFiles: Map<String, VirtualFile>)

/** Needs a read action. Goes through documents so unsaved edits count */
internal fun readWorkspace(project: Project): Workspace {
    val found = mutableListOf<VirtualFile>()
    ProjectFileIndex.getInstance(project).iterateContent { file ->
        if (!file.isDirectory && file.extension.equals("md", ignoreCase = true) && "/node_modules/" !in file.path) found += file
        true
    }
    val documents = FileDocumentManager.getInstance()
    val virtual   = found.sortedBy { it.path }.associateBy { it.path.removePrefix("${project.basePath}/") }
    val files     = virtual.mapNotNull { (path, file) -> documents.getDocument(file)?.let { ScannedFile(path, it.text.split("\n")) } }
    return Workspace(files, virtual)
}

private fun scan(project: Project): Workspace =
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
        ThrowableComputable<Workspace, RuntimeException> {
            ReadAction.compute<Workspace, RuntimeException> { readWorkspace(project) }
        },
        "CL: Scanning workspace", false, project,
    )

private class WorkspaceEntryRenderer : SimpleListCellRenderer<WorkspaceEntry>() {
    override fun customize(list: JList<out WorkspaceEntry>, value: WorkspaceEntry?, index: Int, selected: Boolean, hasFocus: Boolean) {
        text = value?.label ?: ""
    }
}

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun showEntries(project: Project, editor: Editor, workspace: Workspace, title: String, entries: List<WorkspaceEntry>, whenEmpty: String) {
    if (entries.isEmpty()) return tell(editor, whenEmpty)
    JBPopupFactory.getInstance()
        .createPopupChooserBuilder(entries)
        .setTitle(title)
        .setRenderer(WorkspaceEntryRenderer())
        .setNamerForFiltering { it.label }
        .setItemChosenCallback { entry ->
            workspace.virtualFiles[entry.path]?.let { OpenFileDescriptor(project, it, entry.line, 0).navigate(true) }
        }
        .createPopup()
        .showInBestPositionFor(editor)
}

abstract class WorkspaceListAction(
    text: String, description: String, private val title: String, private val whenEmpty: String,
    private val entries: (List<ScannedFile>) -> List<WorkspaceEntry>,
) : MarkdownEditorAction(text, description) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor    = e.getData(CommonDataKeys.EDITOR) ?: return
        val project   = e.project ?: return
        val workspace = scan(project)
        showEntries(project, editor, workspace, title, entries(workspace.files), whenEmpty)
    }
}

/** Asks which tag, person or colour, then lists its items */
abstract class WorkspacePickAction(
    text: String, description: String, private val title: String, private val prompt: String, private val whenEmpty: String,
    private val groups: (List<ScannedFile>) -> Map<String, List<WorkspaceEntry>>,
) : MarkdownEditorAction(text, description) {
    private fun normal(input: String) = input.trim().removePrefix("#").removePrefix("@")

    override fun actionPerformed(e: AnActionEvent) {
        val editor    = e.getData(CommonDataKeys.EDITOR) ?: return
        val project   = e.project ?: return
        val workspace = scan(project)
        val found     = groups(workspace.files)
        if (found.isEmpty()) return tell(editor, whenEmpty)
        val answer = Messages.showInputDialog(project, prompt, title, null, found.keys.first(), oneOfValidator(found.keys) { listOf(normal(it)) }) ?: return
        showEntries(project, editor, workspace, title, found[normal(answer)] ?: return, whenEmpty)
    }
}

abstract class WorkspaceDocumentAction(
    text: String, description: String, private val fileName: String, private val whenEmpty: String,
    private val report: (List<ScannedFile>) -> String?,
) : MarkdownEditorAction(text, description) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val content = report(scan(project).files) ?: return tell(editor, whenEmpty)
        FileEditorManager.getInstance(project).openFile(LightVirtualFile(fileName, content), true)
    }
}

class TodayViewAction : WorkspaceListAction(
    "CL: Today View", "List the open items due today or overdue in every markdown file",
    "Today View", "CL: Nothing due today",
    { todayEntries(it, listPrefix, LocalDate.now()) },
)

class FindDeadLinksAction : WorkspaceListAction(
    "CL: Find Dead Links", "List every section or file link that points at nothing, in every markdown file",
    "Dead Links", "CL: No dead links found",
    { deadLinks(it) },
)

class SearchItemsWorkspaceAction : WorkspaceListAction(
    "CL: Search Items (Workspace)", "Type to find an item in any markdown file and jump to it",
    "Search Items (Workspace)", "CL: No items found in the workspace",
    { workspaceItems(it, listPrefix) },
)

class FilterSectionsWorkspaceAction : WorkspaceListAction(
    "CL: Filter Sections (Workspace)", "Type to find a section in any markdown file and jump to it",
    "Filter Sections (Workspace)", "CL: No sections found in the workspace",
    { workspaceSections(it) },
)

class FilterByTagWorkspaceAction : WorkspacePickAction(
    "CL: Filter by Tag (Workspace)", "List the items with a chosen tag in every markdown file",
    "Filter by Tag (Workspace)", "Tag:", "CL: No #tags found in the workspace",
    { workspaceTags(it, listPrefix) },
)

class FilterByMentionWorkspaceAction : WorkspacePickAction(
    "CL: Filter by Mention (Workspace)", "List the items mentioning a chosen person in every markdown file",
    "Filter by Mention (Workspace)", "Person:", "CL: No @mentions found in the workspace",
    { workspaceMentions(it, listPrefix) },
)

class FilterByColourLabelWorkspaceAction : WorkspacePickAction(
    "CL: Filter by Colour Label (Workspace)", "List the items with a chosen colour label in every markdown file",
    "Filter by Colour Label (Workspace)", "Colour:", "CL: No colour labels found in the workspace",
    { workspaceColours(it, listPrefix) },
)

class ShowTagReportWorkspaceAction : WorkspaceDocumentAction(
    "CL: Show Tag Report (Workspace)", "Open a report of each tag's use per markdown file",
    "Workspace tag report.md", "CL: No #tags found in the workspace",
    { workspaceTagReport(it, listPrefix) },
)

class ShowWorkspaceStatisticsAction : WorkspaceDocumentAction(
    "CL: Show Workspace Statistics", "Open totals across every markdown file",
    "Workspace statistics.md", "CL: No markdown files found in the workspace",
    { workspaceStatistics(it, listPrefix) },
)
