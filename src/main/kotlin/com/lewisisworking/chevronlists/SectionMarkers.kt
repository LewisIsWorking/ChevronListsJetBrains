/**
 * SectionMarkers.kt
 * Pure logic for the section-wide marker commands and votes. Each one changes
 * one part of an item through splitItem and joinItem, so markers are found in
 * any order and an item the command does not touch keeps its exact text. No
 * IntelliJ Platform imports - fully testable with plain JUnit.
 */
package com.lewisisworking.chevronlists

private val DUE_DATE = Regex("""@\d{4}-\d{2}-\d{2}""")

private fun editParts(content: String, change: (ItemParts) -> ItemParts): String {
    val parts = splitItem(content)
    val changed = change(parts)
    return if (changed == parts) content else joinItem(changed)
}

/** Pure: [content] ticked, gaining a checkbox when it has none */
fun markItemDone(content: String): String = editParts(content) { it.copy(check = "[x]") }

/** Pure: [content] unticked when it is done, otherwise unchanged */
fun markItemUndone(content: String): String =
    editParts(content) { if (it.check.equals("[x]", ignoreCase = true)) it.copy(check = "[ ]") else it }

/** Pure: [content] without its checkbox */
fun removeItemCheckbox(content: String): String = editParts(content) { it.copy(check = "") }

/** Pure: [content] without its priority marker; an exclamation mark in the words stays */
fun clearItemPriority(content: String): String = editParts(content) { it.copy(priority = "") }

/** Pure: [content] without its @YYYY-MM-DD due dates; a creation date stays */
fun clearItemDueDates(content: String): String = editParts(content) { it.copy(body = DUE_DATE.replace(it.body, "")) }

/** Pure: [content] without its // comment */
fun stripItemComment(content: String): String = editParts(content) { it.copy(comment = null) }

/** Pure: [content] with its votes changed by [delta], never below none */
fun changeItemVotes(content: String, delta: Int): String = editParts(content) { parts ->
    parts.copy(vote = ((parts.vote ?: 0) + delta).takeIf { it > 0 })
}

/** Pure: [lines] with [change] applied to the content of each item line */
fun mapSectionItems(lines: List<String>, listPrefix: String, change: (String) -> String): List<String> =
    lines.map { transformItemLine(it, listPrefix, change) ?: it }
