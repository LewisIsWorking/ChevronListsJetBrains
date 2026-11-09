/**
 * WorkspaceRenameActions.kt
 * Thin actions over WorkspaceRenames.kt. Every changed file is updated in one
 * undoable command; a read-only file is skipped and counted in the message.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

/** Applies [changes], returning the ones made and how many read-only files were skipped */
private fun applyAll(project: Project, title: String, workspace: Workspace, changes: List<FileChange>): Pair<List<FileChange>, Int> {
    val documents = FileDocumentManager.getInstance()
    val writable  = changes.mapNotNull { change ->
        workspace.virtualFiles[change.path]?.let(documents::getDocument)?.takeIf { it.isWritable }?.let { change to it }
    }
    WriteCommandAction.runWriteCommandAction(project, title, null, {
        for ((change, document) in writable) replaceChangedLines(document, change.before, change.after)
    })
    return writable.map { it.first } to changes.size - writable.size
}

private fun skippedNote(skipped: Int) = if (skipped > 0) " (${plural(skipped, "read-only file")} skipped)" else ""

class RenameTagWorkspaceAction : MarkdownEditorAction(
    "CL: Rename Tag (Workspace)", "Rename a tag on every item in every markdown file of the project"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor    = e.getData(CommonDataKeys.EDITOR) ?: return
        val project   = e.project ?: return
        val workspace = scanWorkspace(project)
        val tags      = workspace.files.flatMap { itemTags(it.lines, listPrefix) }.distinct().sorted()
        if (tags.isEmpty()) return tell(editor, "CL: No #tags found in workspace")
        val (old, new) = askTagRename(e, tags) ?: return
        val changes = workspaceTagRenames(workspace.files, listPrefix, old, new)
        val (made, skipped) = applyAll(project, "Rename Tag (Workspace)", workspace, changes)
        tell(editor, "CL: Renamed #$old to #$new on ${plural(made.sumOf { it.changed }, "line")} across ${plural(made.size, "file")}${skippedNote(skipped)}")
    }
}

class RenameSectionWorkspaceAction : MarkdownEditorAction(
    "CL: Rename Section (Workspace)", "Rename this section and update every [[link]] to it in the project"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = documentLines(editor)
        val (header, oldName) = sectionNameAt(lines, editor.caretModel.logicalPosition.line)
            ?: return tell(editor, "CL: No section found at cursor")
        val newName = Messages.showInputDialog(project, "Rename section (updates all [[links]] in the project):",
            "Rename Section (Workspace)", null, oldName, null)?.trim()
        if (newName.isNullOrEmpty() || newName == oldName) return
        val workspace = scanWorkspace(project)
        val current   = FileDocumentManager.getInstance().getFile(editor.document)
        val path      = workspace.virtualFiles.entries.firstOrNull { it.value == current }?.key
            ?: return tell(editor, "CL: This file is not part of the project")
        val changes = workspaceSectionRenames(workspace.files, path, header, newName)
        val (made, skipped) = applyAll(project, "Rename Section (Workspace)", workspace, changes)
        tell(editor, "CL: Renamed \"$oldName\" to \"$newName\" across ${plural(made.size, "file")}${skippedNote(skipped)}")
    }
}
