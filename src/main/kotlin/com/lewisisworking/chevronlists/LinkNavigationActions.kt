/**
 * LinkNavigationActions.kt
 * Thin actions over LinkNavigation.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

private fun caretText(editor: Editor) = documentLines(editor)[caretLine(editor)]

private fun askOne(project: Project, title: String, prompt: String, choices: List<String>): String? =
    Messages.showInputDialog(project, prompt, title, null, choices.first(),
        oneOfValidator(choices.toSet()) { listOf(it.trim()) })?.trim()

class GoToLinkedSectionAction : MarkdownEditorAction(
    "CL: Go to Linked Section", "Jump to the section named by the [[link]] at the cursor"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val target = sectionLinkAt(caretText(editor), editor.caretModel.logicalPosition.column)
            ?: return tell(editor, "CL: No [[link]] found at cursor position")
        val header = sectionNamed(documentLines(editor), target)
            ?: return tell(editor, "CL: Section \"$target\" not found in this file")
        editor.caretModel.moveToLogicalPosition(LogicalPosition(header, 0))
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }
}

/** The link's file beside this one, or failing that anywhere in the project, as Find Dead Links counts it */
private fun linkedFile(project: Project, from: VirtualFile?, filename: String): VirtualFile? =
    from?.parent?.findFileByRelativePath(filename)
        ?: scanWorkspace(project).let { workspace ->
            workspace.files.firstOrNull { it.name == filename }?.let { workspace.virtualFiles[it.path] }
        }

class GoToLinkedFileAction : MarkdownEditorAction(
    "CL: Go to Linked File", "Open the file named by the [[file:...]] link at the cursor, at its section if it names one"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val link    = fileLinkAt(caretText(editor), editor.caretModel.logicalPosition.column)
            ?: return tell(editor, "CL: No [[file:...]] link found at cursor")
        val documents = FileDocumentManager.getInstance()
        val file      = linkedFile(project, documents.getFile(editor.document), link.filename)
            ?: return tell(editor, "CL: Could not open file \"${link.filename}\"")
        val header = link.section?.let { section -> documents.getDocument(file)?.text?.split("\n")?.let { sectionNamed(it, section) } }
        if (link.section != null && header == null) tell(editor, "CL: Section \"${link.section}\" not found in ${link.filename}")
        OpenFileDescriptor(project, file, header ?: 0, 0).navigate(true)
    }
}

class InsertFileSectionLinkAction : MarkdownEditorAction(
    "CL: Insert File Section Link", "Insert a link to a section of another markdown file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor    = e.getData(CommonDataKeys.EDITOR) ?: return
        val project   = e.project ?: return
        val workspace = scanWorkspace(project)
        val current   = FileDocumentManager.getInstance().getFile(editor.document)
        val others    = workspace.files.filter { workspace.virtualFiles[it.path] != current }
        if (others.isEmpty()) return tell(editor, "CL: No other markdown files found in workspace")
        val title  = "Insert File Section Link"
        val path   = askOne(project, title, "File to link to:", others.map { it.path }) ?: return
        val chosen = others.firstOrNull { it.path == path } ?: return
        val names  = sectionNames(chosen.lines).distinct()
        if (names.isEmpty()) return tell(editor, "CL: No sections found in ${chosen.name}")
        val section = askOne(project, title, "Section of ${chosen.name}:", names) ?: return
        val link    = fileSectionLink(chosen.name, section)
        WriteCommandAction.runWriteCommandAction(project) {
            val offset = editor.caretModel.offset
            editor.document.insertString(offset, link)
            editor.caretModel.moveToOffset(offset + link.length)
        }
    }
}

class FindInSectionsAction : MarkdownEditorAction(
    "CL: Find in Sections", "List the items of this file that contain a search term"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val term   = Messages.showInputDialog(e.project, "Search item content:", "Find in Sections", null)
        if (term.isNullOrBlank()) return
        val found = itemsMatching(documentLines(editor), listPrefix, term)
        showReport(editor, "${plural(found.size, "item")} matching \"$term\"", found, "CL: No items matching \"$term\"")
    }
}

class CopyItemAsRichTextAction : MarkdownEditorAction(
    "CL: Copy Item as Rich Text", "Copy this item with its markers written out in readable form"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val text   = richItemText(caretText(editor), listPrefix)
            ?: return tell(editor, "CL: Place cursor on a chevron item to copy")
        CopyPasteManager.getInstance().setContents(StringSelection(text))
        tell(editor, "CL: Copied as rich text")
    }
}

class ShowDependenciesAction : MarkdownEditorAction(
    "CL: Show Dependencies", "List the sections that declare a dependency on another section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val found  = sectionDependencies(documentLines(editor))
        showReport(editor, plural(found.size, "dependency relationship"), found, "CL: No dependencies found (use >>depends:SectionName in a section)")
    }
}
