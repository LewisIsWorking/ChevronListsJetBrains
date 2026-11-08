/**
 * ItemFilterActions.kt
 * IntelliJ bridge for ItemFilters.kt.
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

private fun askNumber(e: AnActionEvent, prompt: String, title: String, error: String, allowed: IntRange): Int? =
    Messages.showInputDialog(e.project, prompt, title, null, "", numberValidator(error) { it in allowed })?.trim()?.toIntOrNull()

private fun colourValidator(colours: Set<String>) = object : InputValidatorEx {
    override fun getErrorText(input: String) =
        if (input.trim().lowercase() in colours) null else "Enter one of: ${colours.joinToString(", ")}"
    override fun checkInput(input: String) = getErrorText(input) == null
    override fun canClose(input: String) = checkInput(input)
}

class FilterByPriorityAction : MarkdownEditorAction(
    "CL: Filter by Priority", "List the items with a chosen priority"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        val none   = "CL: No priority items found (use !, !!, or !!! at the start of item content)"
        if (itemsWithPriority(lines, listPrefix, 0).isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, none)
            return
        }
        val level = askNumber(e, "Priority: 0 for all, 1 for !, 2 for !!, 3 for !!!", "Filter by Priority", "Enter 0, 1, 2 or 3", 0..3) ?: return
        showReport(editor, "Filter by Priority", itemsWithPriority(lines, listPrefix, level), "CL: No items have that priority")
    }
}

class FilterStarredItemsAction : MarkdownEditorAction(
    "CL: Filter Starred Items", "List the starred items in this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Starred Items", starredItems(documentLines(editor), listPrefix), "CL: No starred items found")
    }
}

class FilterFlaggedItemsAction : MarkdownEditorAction(
    "CL: Filter Flagged Items", "List the flagged items in this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Flagged Items", flaggedItems(documentLines(editor), listPrefix), "CL: No flagged items found")
    }
}

class ShowExpiredItemsAction : MarkdownEditorAction(
    "CL: Show Expired Items", "List the items whose expiry date has passed"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(
            editor, "Expired Items", expiredItems(documentLines(editor), listPrefix, LocalDate.now().toString()),
            "CL: No expired items found (@expires:YYYY-MM-DD)"
        )
    }
}

class FilterByRatingAction : MarkdownEditorAction(
    "CL: Filter by Rating", "List the items rated at least a chosen number of stars"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        if (ratedItems(lines, listPrefix, 1).isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "CL: No rated items found (use ★N in item content)")
            return
        }
        val minimum = askNumber(e, "Show items rated at least (1 to 5):", "Filter by Rating", "Enter a number from 1 to 5", 1..5) ?: return
        showReport(editor, "Items Rated $minimum+", ratedItems(lines, listPrefix, minimum), "CL: No items rated $minimum+ stars")
    }
}

class FilterByColourLabelAction : MarkdownEditorAction(
    "CL: Filter by Colour Label", "List the items with a chosen colour label"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        val counts = colourCounts(lines, listPrefix)
        if (counts.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "CL: No colour-labelled items found (use {red}, {green}, etc.)")
            return
        }
        val prompt = "Colour (" + counts.joinToString(", ") { (colour, n) -> "$colour: $n" } + "):"
        val colour = Messages.showInputDialog(
            e.project, prompt, "Filter by Colour Label", null, counts.first().first,
            colourValidator(counts.map { it.first }.toSet())
        )?.trim()?.lowercase() ?: return
        showReport(editor, "Items Labelled {$colour}", itemsWithColour(lines, listPrefix, colour), "CL: No items labelled {$colour}")
    }
}

class ShowVoteLeaderboardAction : MarkdownEditorAction(
    "CL: Show Vote Leaderboard", "List the voted items in this file, most votes first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Vote Leaderboard", voteLeaderboard(documentLines(editor), listPrefix), "CL: No voted items found (use +N in item content)")
    }
}

class ShowSectionVoteLeaderboardAction : MarkdownEditorAction(
    "CL: Show Vote Leaderboard (Section)", "List the voted items in this section, most votes first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.editor() ?: return
        val entries = sectionVoteLeaderboard(documentLines(editor), editor.caretModel.logicalPosition.line, listPrefix)
        if (entries == null) {
            HintManager.getInstance().showInformationHint(editor, "CL: No section found at cursor")
            return
        }
        showReport(editor, "Section Vote Leaderboard", entries, "CL: No voted items in this section (use +N)")
    }
}

class ShowDuplicateItemsAction : MarkdownEditorAction(
    "CL: Show Duplicate Items (File)", "List the items that appear more than once in this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Duplicate Items", duplicateItems(documentLines(editor), listPrefix), "CL: No duplicate items found in this file")
    }
}
