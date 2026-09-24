/**
 * ListCommandActions.kt
 * IntelliJ bridge for ListCommands.kt: section-wide renumber and list-type
 * conversion. All decisions are made by the pure functions; this class only
 * reads the document and rewrites the changed span.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

abstract class ChevronSectionListAction(
    actionText:        String,
    actionDescription: String,
    private val change: (lines: List<String>, listPrefix: String) -> List<String>
) : AnAction(actionText, actionDescription, null) {

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

        val before = document.text.split("\n")
        val line   = document.getLineNumber(editor.caretModel.offset)
        val after  = applyToSection(before, line) { change(it, prefix) } ?: return
        val span   = changedSpan(before, after) ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            val start = document.getLineStartOffset(span.first)
            val end   = document.getLineEndOffset(span.last)
            document.replaceString(start, end, after.subList(span.first, span.last + 1).joinToString("\n"))
        }
    }
}

class RenumberItemsAction : ChevronSectionListAction(
    actionText        = "CL: Renumber Items",
    actionDescription = "Number every list in this section from 1; nested lists are counted separately",
    change            = { lines, _ -> renumberLines(lines) }
)

class ConvertBulletsToNumberedAction : ChevronSectionListAction(
    actionText        = "CL: Convert Bullets to Numbered List",
    actionDescription = "Turn every bullet in this section into a numbered item",
    change            = { lines, prefix -> bulletsToNumbered(lines, prefix) }
)

class ConvertNumberedToBulletsAction : ChevronSectionListAction(
    actionText        = "CL: Convert Numbered List to Bullets",
    actionDescription = "Turn every numbered item in this section into a bullet",
    change            = { lines, prefix -> numberedToBullets(lines, prefix) }
)
