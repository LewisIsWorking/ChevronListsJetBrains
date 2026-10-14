/**
 * StatisticsActions.kt
 * IntelliJ bridge for Statistics.kt: Quick Stats shows the caret section's
 * counts in a hint, and Show File Statistics shows the whole file's in a dialog.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import java.time.LocalDate

private val listPrefix: String get() = ChevronListsSettings.getInstance().state.listPrefix
private val today: String get() = LocalDate.now().toString()

class QuickStatsAction : MarkdownEditorAction(
    "CL: Quick Stats",
    "Show the caret section's items, done count, words and tags"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = editor.document.text.split("\n")
        val header = sectionHeaderAt(lines, editor.caretModel.logicalPosition.line)
        val text   = if (header < 0) "CL: No section found at the caret"
                     else "CL: " + quickStatsText(sectionStats(lines, header, listPrefix, today))
        HintManager.getInstance().showInformationHint(editor, text)
    }
}

class ShowStatisticsAction : MarkdownEditorAction(
    "CL: Show File Statistics",
    "Show item, word, done and tag counts for every section in this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val stats  = fileStats(editor.document.text.split("\n"), listPrefix, today)
        val text   = if (stats.sections.isEmpty()) "This file has no > sections." else fileStatsReport(stats)
        Messages.showInfoMessage(e.project, text, "File Statistics")
    }
}
