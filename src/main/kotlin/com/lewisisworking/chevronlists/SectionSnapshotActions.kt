/**
 * SectionSnapshotActions.kt
 * Thin actions over SectionSnapshots.kt and the project's ChevronSnapshots.
 * VS Code's snapshot quick pick is a validated input box here.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightVirtualFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private const val NO_SECTION    = "CL: No section found at cursor"
private const val NO_ITEM       = "CL: Place cursor on a chevron item"
private const val NO_SNAPSHOTS  = "CL: No snapshots for this file"
private const val NO_ITEM_SAVED = "CL: No snapshot stored - use CL: Snapshot Item first"

private val SAVED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

private fun fileKey(editor: Editor): String? = FileDocumentManager.getInstance().getFile(editor.document)?.url

private fun snapshotsOf(project: Project) = project.service<ChevronSnapshots>()

private fun openDocument(project: Project, name: String, content: String) {
    FileEditorManager.getInstance(project).openFile(LightVirtualFile(name, content), true)
}

class ArchiveSectionAction : MarkdownEditorAction(
    "CL: Archive Section", "Move this section to the end of the Archive's items"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = documentLines(editor)
        val header = headerAtOrAbove(before, caretLine(editor)) ?: return tell(editor, NO_SECTION)
        val after  = archiveSection(before, caretLine(editor)) ?: return tell(editor, "CL: This section is already archived")
        WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }
        tell(editor, "CL: Archived \"${parseHeader(before[header])!!.content.trim()}\"")
    }
}

class ShowArchiveAction : MarkdownEditorAction(
    "CL: Show Archive", "List the items of the Archive section and jump to one"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val entries = archiveEntries(documentLines(editor), listPrefix)
            ?: return tell(editor, "CL: No > Archive section found in this file")
        showReport(editor, "Archive", entries, "CL: Archive section is empty")
    }
}

class SnapshotSectionAction : MarkdownEditorAction(
    "CL: Snapshot Section", "Save this section under a name so it can be restored later"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = documentLines(editor)
        val header  = headerAtOrAbove(lines, caretLine(editor)) ?: return tell(editor, NO_SECTION)
        val file    = fileKey(editor) ?: return tell(editor, "CL: Save this file before taking a snapshot")
        val name = Messages.showInputDialog(project, "Snapshot name:", "Snapshot Section", null,
            parseHeader(lines[header])!!.content.trim(), null)?.trim()
        if (name.isNullOrEmpty()) return
        val text = sectionSnapshotText(lines, caretLine(editor)) ?: return
        snapshotsOf(project).add(ChevronSnapshots.Snapshot(name, file, text, LocalDateTime.now().format(SAVED_AT)))
        tell(editor, "CL: Snapshot \"$name\" saved")
    }
}

class RestoreSnapshotAction : MarkdownEditorAction(
    "CL: Restore Snapshot", "Replace this section with a saved snapshot"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val saved   = fileKey(editor)?.let { snapshotsOf(project).forFile(it) }.orEmpty()
        if (saved.isEmpty()) return tell(editor, NO_SNAPSHOTS)
        val before = documentLines(editor)
        val names  = saved.map { it.name }.toSet()
        val name = Messages.showInputDialog(project, "Snapshot to restore:", "Restore Snapshot", null, saved.last().name,
            oneOfValidator(names) { listOf(it.trim()) })?.trim() ?: return
        val chosen = saved.lastOrNull { it.name == name } ?: return
        val after  = restoreSnapshotText(before, caretLine(editor), chosen.content) ?: return tell(editor, NO_SECTION)
        WriteCommandAction.runWriteCommandAction(project) { replaceChangedLines(editor.document, before, after) }
        tell(editor, "CL: Snapshot \"$name\" restored")
    }
}

class ListSnapshotsAction : MarkdownEditorAction(
    "CL: List Snapshots", "Open a list of the snapshots saved for this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val saved   = fileKey(editor)?.let { snapshotsOf(project).forFile(it) }.orEmpty()
        if (saved.isEmpty()) return tell(editor, NO_SNAPSHOTS)
        openDocument(project, "Snapshots.md", snapshotListing(saved.map { it.name to it.savedAt }))
    }
}

class SnapshotItemAction : MarkdownEditorAction(
    "CL: Snapshot Item", "Remember this item so it can be compared after editing"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val content = itemContent(documentLines(editor)[caretLine(editor)], listPrefix) ?: return tell(editor, NO_ITEM)
        snapshotsOf(project).itemSnapshot = content
        tell(editor, "CL: Snapshot saved - \"${content.take(50)}\"")
    }
}

class DiffItemWithSnapshotAction : MarkdownEditorAction(
    "CL: Diff Item with Snapshot", "Open how this item differs from the item snapshot"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val before  = snapshotsOf(project).itemSnapshot ?: return tell(editor, NO_ITEM_SAVED)
        val current = itemContent(documentLines(editor)[caretLine(editor)], listPrefix) ?: return tell(editor, NO_ITEM)
        openDocument(project, "Item snapshot diff.md", itemSnapshotDiff(before, current))
    }
}
