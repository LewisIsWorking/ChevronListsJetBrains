/**
 * FileMoveActions.kt
 * Thin actions over FileMoves.kt. VS Code's quick picks of files and sections
 * are validated input boxes here. Both files change in one undoable command.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private const val NO_FILES = "CL: No other markdown files found in workspace"

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

private fun askOne(project: Project, title: String, prompt: String, choices: List<String>): String? =
    Messages.showInputDialog(project, prompt, title, null, choices.first(),
        oneOfValidator(choices.toSet()) { listOf(it.trim()) })?.trim()

private class Target(val path: String, val lines: List<String>, val document: Document)

/** Asks for another markdown file of the project. Null if there is none, it cannot be edited or the user cancels */
private fun askTargetFile(editor: Editor, project: Project, title: String, prompt: String): Target? {
    val workspace = scanWorkspace(project)
    val current   = FileDocumentManager.getInstance().getFile(editor.document)
    val others    = workspace.files.filter { workspace.virtualFiles[it.path] != current }
    if (others.isEmpty()) { tell(editor, NO_FILES); return null }
    val path    = askOne(project, title, prompt, others.map { it.path }) ?: return null
    val chosen  = others.firstOrNull { it.path == path } ?: return null
    val document = workspace.virtualFiles[path]?.let { FileDocumentManager.getInstance().getDocument(it) } ?: return null
    if (!document.isWritable) { tell(editor, "CL: $path cannot be edited; nothing was changed"); return null }
    return Target(path, chosen.lines, document)
}

class MoveItemToFileAction : MarkdownEditorAction(
    "CL: Move Item to File", "Move this item and everything nested under it to a section of another file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = documentLines(editor)
        val range   = itemBlockRange(lines, caretLine(editor), listPrefix)
            ?: return tell(editor, "CL: Place cursor on a chevron item to move it")
        val target = askTargetFile(editor, project, "Move Item to File", "Move to file:") ?: return
        val names  = sectionNames(target.lines)
        if (names.isEmpty()) return tell(editor, "CL: No sections found in ${target.path}")
        val name   = askOne(project, "Move Item to File", "Move to section:", names) ?: return
        val header = sectionNamed(target.lines, name) ?: return
        val added  = withBlockInSection(target.lines, header, lines.slice(range))
        val rest   = lines.filterIndexed { index, _ -> index !in range }
        WriteCommandAction.runWriteCommandAction(project, "Move Item to File", null, {
            replaceChangedLines(target.document, target.lines, added)
            replaceChangedLines(editor.document, lines, rest)
        })
        tell(editor, "CL: Moved item to \"$name\" in ${target.path}")
    }
}

class DuplicateSectionToFileAction : MarkdownEditorAction(
    "CL: Duplicate Section to File", "Copy this section to the end of another file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = documentLines(editor)
        val header  = headerAtOrAbove(lines, caretLine(editor)) ?: return tell(editor, "CL: No section found at cursor")
        val name    = parseHeader(lines[header])!!.content.trim()
        val target  = askTargetFile(editor, project, "Duplicate Section to File", "Copy \"$name\" to file:") ?: return
        val added   = withSectionAtEnd(target.lines, sectionBlock(lines, header))
        WriteCommandAction.runWriteCommandAction(project, "Duplicate Section to File", null, {
            replaceChangedLines(target.document, target.lines, added)
        })
        tell(editor, "CL: Copied \"$name\" to ${target.path}")
    }
}
