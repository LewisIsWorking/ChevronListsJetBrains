/**
 * DailyNoteActions.kt
 * IntelliJ bridges for DailyNotes.kt: open (or create) today's daily note, and
 * send the item at the caret to today's note under > Inbox. Notes live in the
 * folder set in Settings > Tools > Chevron Lists (the project folder if blank).
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import java.nio.file.Path
import java.time.LocalDate

/** Shared file handling for the two daily note actions */
internal object DailyNoteFiles {

    /** Today's note folder, or null (with a status bar message) when there is none */
    fun folder(project: Project): Path? {
        val folder = dailyNotesFolder(ChevronListsSettings.getInstance().state.dailyNotesFolder, project.basePath)
        if (folder == null) StatusBar.Info.set("CL: Set a daily notes folder in Settings > Tools > Chevron Lists", project)
        return folder
    }

    /** Today's note, created with [initial] if missing. Must run inside a write command. */
    fun findOrCreate(folder: Path, name: String, initial: () -> String): Pair<VirtualFile, Boolean> {
        val dir = VfsUtil.createDirectoryIfMissing(folder.toString())
            ?: error("Cannot create daily notes folder $folder")
        dir.refresh(false, false)
        dir.findChild(name)?.let { return it to false }
        val file = dir.createChildData(this, name)
        VfsUtil.saveText(file, initial())
        return file to true
    }
}

class OpenDailyNoteAction : AnAction(
    "CL: Open Daily Note",
    "Open today's daily note (YYYY-MM-DD.md), creating it from the daily note template",
    null
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val folder  = DailyNoteFiles.folder(project) ?: return
        val state   = ChevronListsSettings.getInstance().state
        val today   = LocalDate.now()
        val file = WriteCommandAction.writeCommandAction(project).withName("Open Daily Note").compute<VirtualFile, RuntimeException> {
            DailyNoteFiles.findOrCreate(folder, dailyNoteFileName(today)) {
                newDailyNote(state.dailyNoteTemplate, state.listPrefix, today)
            }.first
        }
        val lines = FileDocumentManager.getInstance().getDocument(file)?.text?.split("\n").orEmpty()
        val line  = firstBlankItemLine(lines, state.listPrefix)
        val descriptor = if (line == null) OpenFileDescriptor(project, file)
                         else OpenFileDescriptor(project, file, line, lines[line].length)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }
}

class SendToDailyNoteAction : AnAction(
    "CL: Send to Daily Note",
    "Copy the item at the caret to today's daily note, under > Inbox",
    null
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val prefix  = ChevronListsSettings.getInstance().state.listPrefix
        val doc     = editor.document
        val n       = doc.getLineNumber(editor.caretModel.offset)
        val content = itemContent(doc.getText(TextRange(doc.getLineStartOffset(n), doc.getLineEndOffset(n))), prefix)
        if (content == null) {
            StatusBar.Info.set("CL: Place the caret on a chevron item", project)
            return
        }
        val folder = DailyNoteFiles.folder(project) ?: return
        val today  = LocalDate.now()
        val name   = dailyNoteFileName(today)
        WriteCommandAction.runWriteCommandAction(project, "Send to Daily Note", null, {
            val (file, created) = DailyNoteFiles.findOrCreate(folder, name) { sendToInbox(null, content, prefix, today) }
            if (!created) {
                val note   = FileDocumentManager.getInstance().getDocument(file) ?: return@runWriteCommandAction
                val change = minimalReplacement(note.text, sendToInbox(note.text, content, prefix, today))
                if (change != null) note.replaceString(change.start, change.end, change.text)
            }
        })
        StatusBar.Info.set("CL: Sent to $name > Inbox", project)
    }
}
