/**
 * ItemReportActions.kt
 * IntelliJ bridge for ItemReports.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import java.time.LocalDate

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun AnActionEvent.editor(): Editor? = getData(CommonDataKeys.EDITOR)

private fun personOf(input: String) = input.trim().removePrefix("@")

private fun personValidator(names: Set<String>) = object : InputValidatorEx {
    override fun getErrorText(input: String) =
        if (personOf(input) in names) null else "Enter one of: ${names.joinToString(", ")}"
    override fun checkInput(input: String) = getErrorText(input) == null
    override fun canClose(input: String) = checkInput(input)
}

class ShowUpcomingDueDatesAction : MarkdownEditorAction(
    "CL: Show Upcoming Due Dates", "List the items with due dates, soonest first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(
            editor, "Due Dates", upcomingDueDates(documentLines(editor), listPrefix, LocalDate.now()),
            "CL: No due dates found (use @YYYY-MM-DD in item content)"
        )
    }
}

class ShowRecurringItemsAction : MarkdownEditorAction(
    "CL: Show Recurring Items", "List the items marked daily, weekly or monthly"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(
            editor, "Recurring Items", recurringItems(documentLines(editor), listPrefix),
            "CL: No recurring items found (use @daily, @weekly, or @monthly in item content)"
        )
    }
}

class ShowOldItemsAction : MarkdownEditorAction(
    "CL: Show Old Items (30+ days)", "List the items created at least 30 days ago"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Old Items", oldItems(documentLines(editor), listPrefix, LocalDate.now()), "CL: No items older than 30 days found")
    }
}

class ShowTimeEstimatesAction : MarkdownEditorAction(
    "CL: Show Time Estimates", "List the items with time estimates, longest first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        showReport(
            editor, timeEstimatesTitle(lines, listPrefix), timeEstimates(lines, listPrefix),
            "CL: No time estimates found (use ~2h, ~30m, or ~1h30m in item content)"
        )
    }
}

class ShowSectionSummaryAction : MarkdownEditorAction(
    "CL: Show Section Summary", "Show the items, words, done count and tags of this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.editor() ?: return
        val message = sectionSummary(documentLines(editor), editor.caretModel.logicalPosition.line, listPrefix)
        HintManager.getInstance().showInformationHint(editor, message ?: "CL: No section found at cursor")
    }
}

class CountItemsByTagAction : MarkdownEditorAction(
    "CL: Count Items by Tag", "List each tag with how many items carry it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Items by Tag", itemCountsByTag(documentLines(editor), listPrefix), "CL: No #tags found in this file")
    }
}

class ShowTagStatsAction : MarkdownEditorAction(
    "CL: Show Tag Stats", "List each tag with how many of its items are done"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Tag Stats", tagStats(documentLines(editor), listPrefix), "CL: No #tags found in this file")
    }
}

class FilterByMentionAction : MarkdownEditorAction(
    "CL: Filter by Mention", "List the items that mention a chosen person"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        val counts = mentionCounts(lines, listPrefix)
        if (counts.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "CL: No @mentions found (use @Name in item content)")
            return
        }
        val prompt = "Person (" + counts.joinToString(", ") { (name, n) -> "$name: $n" } + "):"
        val answer = Messages.showInputDialog(
            e.project, prompt, "Filter by Mention", null, counts.first().first,
            personValidator(counts.map { it.first }.toSet())
        ) ?: return
        val name = personOf(answer)
        showReport(editor, "Items Mentioning @$name", itemsMentioning(lines, listPrefix, name), "CL: No items mention @$name")
    }
}

class ShowMentionsReportAction : MarkdownEditorAction(
    "CL: Show Mentions Report", "List each person mentioned with how many of their items are done"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Mentions Report", mentionStats(documentLines(editor), listPrefix), "CL: No @mentions found (use @Name in item content)")
    }
}
