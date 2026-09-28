/**
 * ArchiveDoneAction.kt
 * IntelliJ bridge for ArchiveDone.kt: moves the done items of the section at the
 * caret into the archive, rewriting only the text that changed.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory

class ArchiveDoneAction : AnAction(
    "CL: Archive Done Items",
    "Move the done items of this section, with anything nested under them, into the > Archive section",
    null
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val prefix   = ChevronListsSettings.getInstance().state.listPrefix

        val before = document.text
        val result = computeArchiveDone(before.split("\n"), document.getLineNumber(editor.caretModel.offset), prefix)
        if (result == null) {
            note(editor, "No done items to archive in this section")
            return
        }
        val change = minimalReplacement(before, result.lines.joinToString("\n")) ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(change.start, change.end, change.text)
        }
        note(editor, "Archived ${result.archived} done item${if (result.archived == 1) "" else "s"}")
    }

    /** A short note above the caret (skipped in headless tests, which cannot show one) */
    private fun note(editor: Editor, message: String) {
        if (ApplicationManager.getApplication().isUnitTestMode) return
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
            .createBalloon()
            .show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.above)
    }
}
