/**
 * LockActions.kt
 * Lock and Unlock Section, and the warning when a save changes a locked section.
 * VS Code warns on every save; an IDE can save on its own at any moment, so here
 * only a save that changes a locked section warns.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import java.io.IOException

private fun changeSectionLock(
    e: AnActionEvent,
    title: String,
    change: (List<String>, Int) -> List<String>?,
    unchanged: String,
    done: String,
) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val hints  = HintManager.getInstance()
    val lines  = documentLines(editor)
    val header = headerAtOrAbove(lines, editor.caretModel.logicalPosition.line)
        ?: return hints.showInformationHint(editor, "CL: No section found at cursor")
    val after  = change(lines, header) ?: return hints.showInformationHint(editor, unchanged)
    WriteCommandAction.runWriteCommandAction(e.project, title, null, { replaceChangedLines(editor.document, lines, after) })
    hints.showInformationHint(editor, done)
}

class LockSectionAction : MarkdownEditorAction(
    "CL: Lock Section", "Mark this section as locked, so that saving an edit to it warns"
) {
    override fun actionPerformed(e: AnActionEvent) = changeSectionLock(e, "Lock Section", ::withSectionLocked,
        "CL: Section is already locked", "CL: Section locked - saving an edit to it will warn about it")
}

class UnlockSectionAction : MarkdownEditorAction(
    "CL: Unlock Section", "Remove the lock from this section"
) {
    override fun actionPerformed(e: AnActionEvent) = changeSectionLock(e, "Unlock Section", ::withSectionUnlocked,
        "CL: Section is not locked", "CL: Section unlocked")
}

/** Unlocks the section called [name], looked up now because the document may have changed since the warning */
internal fun unlockSectionNamed(project: Project?, document: Document, name: String) {
    val lines  = document.text.split("\n")
    val header = sectionNamed(lines, name) ?: return
    val after  = withSectionUnlocked(lines, header) ?: return
    WriteCommandAction.runWriteCommandAction(project, "Unlock Section", null, { replaceChangedLines(document, lines, after) })
}

class LockedSectionSaveListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        if (!document.charsSequence.contains(LOCK_MARKER)) return
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!file.isValid || !file.name.endsWith(".md")) return
        val saved = try {
            file.contentsToByteArray().toString(file.charset).removePrefix("﻿").replace("\r\n", "\n")
        } catch (_: IOException) {
            return
        }
        val project = ProjectLocator.getInstance().guessProjectForFile(file)
        val group   = NotificationGroupManager.getInstance().getNotificationGroup("Chevron Lists")
        for (name in changedLockedSections(saved.split("\n"), document.text.split("\n"))) {
            group.createNotification("CL: \"$name\" is locked - use CL: Unlock Section to edit it", NotificationType.WARNING)
                .addAction(NotificationAction.createSimpleExpiring("Unlock section") { unlockSectionNamed(project, document, name) })
                .notify(project)
        }
    }
}
