/**
 * ItemLifecycleActions.kt
 * IntelliJ bridge for ItemLifecycle.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import java.time.LocalDate

private const val DEFAULT_DAYS = "30"
private const val MATCH_ALL    = "all"
private const val MATCH_ANY    = "any"

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun AnActionEvent.editor(): Editor? = getData(CommonDataKeys.EDITOR)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

private fun hint(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

internal fun oneOfValidator(allowed: Set<String>, parse: (String) -> List<String>) = object : InputValidatorEx {
    override fun getErrorText(input: String): String? {
        val words = parse(input)
        return if (words.isNotEmpty() && allowed.containsAll(words)) null else "Enter from: ${allowed.joinToString(", ")}"
    }
    override fun checkInput(input: String) = getErrorText(input) == null
    override fun canClose(input: String) = checkInput(input)
}

private fun askDays(e: AnActionEvent, prompt: String, title: String): Int? =
    Messages.showInputDialog(e.project, prompt, title, null, DEFAULT_DAYS, numberValidator("Enter a positive integer") { it > 0 })
        ?.trim()?.toIntOrNull()

private fun confirm(e: AnActionEvent, message: String, title: String, verb: String) =
    Messages.showOkCancelDialog(e.project, message, title, verb, "Cancel", null) == Messages.OK

private fun applyLines(e: AnActionEvent, editor: Editor, before: List<String>, after: List<String>) =
    WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }

class ShowItemAgeReportAction : MarkdownEditorAction(
    "CL: Show Item Age Report", "List the stamped items of this file, oldest first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        val today  = LocalDate.now()
        showReport(editor, itemAgeReportTitle(lines, listPrefix, today), itemAgeReport(lines, listPrefix, today), "CL: No items with @created: dates found")
    }
}

class ShowAgeStatsAction : MarkdownEditorAction(
    "CL: Show Age Stats", "Show the oldest, newest and average age of this section's stamped items"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        hint(editor, ageStatsMessage(documentLines(editor), caretLine(editor), listPrefix, LocalDate.now()) ?: "CL: No section found at cursor")
    }
}

class ShowWordCountGoalsAction : MarkdownEditorAction(
    "CL: Show Word Count Goals", "List the sections with a word goal, furthest from it first"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Word Count Goals", wordCountGoals(documentLines(editor), listPrefix), "CL: No word count goals found - add ==N to a section header")
    }
}

class ShowItemComplexityAction : MarkdownEditorAction(
    "CL: Show Item Complexity", "Show how many markers the item at the caret carries"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        hint(editor, itemComplexityMessage(documentLines(editor)[caretLine(editor)], listPrefix) ?: "CL: Place cursor on a chevron item")
    }
}

class FilterByMultipleTagsAction : MarkdownEditorAction(
    "CL: Filter by Multiple Tags", "List the items with all or any of the chosen tags"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        val known  = tagHits(lines, listPrefix).map { it.tag }.toSortedSet()
        if (known.isEmpty()) { hint(editor, "CL: No tags found in this file"); return }
        val tags = parseTagList(
            Messages.showInputDialog(e.project, "Tags to filter by, separated by spaces (${known.joinToString(", ") { "#$it" }}):", "Filter by Multiple Tags", null, "", oneOfValidator(known, ::parseTagList)) ?: return
        )
        val mode = Messages.showInputDialog(e.project, "Match $MATCH_ALL of the tags or $MATCH_ANY of them?", "Filter by Multiple Tags", null, MATCH_ALL, oneOfValidator(setOf(MATCH_ALL, MATCH_ANY)) { listOf(it.trim().lowercase()) }) ?: return
        val matchAll = mode.trim().lowercase() == MATCH_ALL
        val found    = itemsWithTags(lines, listPrefix, tags, matchAll)
        showReport(editor, tagFilterTitle(found.size, tags, matchAll), found, "CL: No items match the selected tags")
    }
}

class GenerateNextOccurrenceAction : MarkdownEditorAction(
    "CL: Generate Next Occurrence", "Add the next occurrence of the recurring item at the caret below it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val line   = caretLine(editor)
        val before = documentLines(editor)
        if (itemContent(before[line], listPrefix) == null) { hint(editor, "CL: Place cursor on a recurring item"); return }
        val next = nextOccurrenceLine(before[line], listPrefix, LocalDate.now())
        if (next == null) { hint(editor, "CL: Item has no @daily/@weekly/@monthly marker"); return }
        WriteCommandAction.runWriteCommandAction(e.project) {
            editor.document.insertString(editor.document.getLineEndOffset(line), "\n$next")
        }
    }
}

class InsertRecurringItemAction : MarkdownEditorAction(
    "CL: Insert Recurring Item", "Add a ready-made daily, weekly or monthly item below the caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val menu   = RECURRING_TEMPLATES.withIndex().joinToString("\n") { (i, t) -> "${i + 1}. ${t.first}" }
        val input  = Messages.showInputDialog(
            e.project, "$menu\n\nWhich item?", "Insert Recurring Item", null, "1",
            numberValidator("Enter 1 to ${RECURRING_TEMPLATES.size}") { it in 1..RECURRING_TEMPLATES.size }
        ) ?: return
        val chosen = RECURRING_TEMPLATES.getOrNull((input.trim().toIntOrNull() ?: return) - 1) ?: return
        val line   = caretLine(editor)
        val item   = ">> $listPrefix ${chosen.second}"
        WriteCommandAction.runWriteCommandAction(e.project) {
            editor.document.insertString(editor.document.getLineEndOffset(line), "\n$item")
            editor.caretModel.moveToLogicalPosition(LogicalPosition(line + 1, item.length))
        }
    }
}

class GroupItemsByMentionAction : MarkdownEditorAction(
    "CL: Group Items by Mention", "Put this section's items under a divider for each person mentioned"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val before = documentLines(editor)
        if (headerAtOrAbove(before, caretLine(editor)) == null) { hint(editor, "CL: No section found at cursor"); return }
        val result = groupItemsByMention(before, caretLine(editor), listPrefix)
        if (result == null) { hint(editor, "CL: No @Mention markers found in section items"); return }
        applyLines(e, editor, before, result.lines)
        hint(editor, "CL: Grouped into ${plural(result.count, "mention group")}")
    }
}

class CollectItemsByTagAction : MarkdownEditorAction(
    "CL: Collect Items by Tag", "Copy every item with a chosen tag into a new results section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val before = documentLines(editor)
        val known  = tagHits(before, listPrefix).map { it.tag }.toSortedSet()
        if (known.isEmpty()) { hint(editor, "CL: No #tags found in this file"); return }
        val tag = parseTagList(
            Messages.showInputDialog(e.project, "Tag to collect (${known.joinToString(", ") { "#$it" }}):", "Collect Items by Tag", null, "", oneOfValidator(known, ::parseTagList)) ?: return
        ).firstOrNull() ?: return
        val result = collectItemsByTag(before, listPrefix, tag)
        if (result == null) { hint(editor, "CL: No items found with #$tag"); return }
        applyLines(e, editor, before, result.lines)
        hint(editor, "CL: Collected ${plural(result.count, "item")} into \"#$tag Results\"")
    }
}

class RemoveOldItemsAction : MarkdownEditorAction(
    "CL: Remove Old Items", "Delete the items created at least a chosen number of days ago"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val days   = askDays(e, "Remove items older than how many days?", "Remove Old Items") ?: return
        val before = documentLines(editor)
        val result = removeOldItems(before, listPrefix, days, LocalDate.now())
        if (result == null) { hint(editor, "CL: No items older than ${plural(days, "day")} found"); return }
        if (!confirm(e, "Remove ${plural(result.count, "item")} older than ${plural(days, "day")}?", "Remove Old Items", "Remove")) return
        applyLines(e, editor, before, result.lines)
        hint(editor, "CL: Removed ${plural(result.count, "item")}")
    }
}

class ArchiveOldDoneItemsAction : MarkdownEditorAction(
    "CL: Archive Old Done Items", "Move done items created at least a chosen number of days ago into the > Archive section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val days   = askDays(e, "Archive done items older than how many days?", "Archive Old Done Items") ?: return
        val before = documentLines(editor)
        val result = archiveOldDoneItems(before, listPrefix, days, LocalDate.now())
        if (result == null) { hint(editor, "CL: No done items older than ${plural(days, "day")} found"); return }
        if (!confirm(e, "Archive ${plural(result.archived, "done item")} older than ${plural(days, "day")}?", "Archive Old Done Items", "Archive")) return
        applyLines(e, editor, before, result.lines)
        hint(editor, "CL: Archived ${plural(result.archived, "item")}")
    }
}
