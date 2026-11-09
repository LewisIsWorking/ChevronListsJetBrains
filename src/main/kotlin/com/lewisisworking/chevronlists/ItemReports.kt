/**
 * ItemReports.kt
 * Pure lists of dated, recurring, estimated, tagged and mentioned items.
 * No IntelliJ imports. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private const val OLD_AFTER_DAYS = 30L
private const val OVERDUE = "⚠️ OVERDUE"

internal val ITEM_DUE_DATE = Regex("""@(\d{4}-\d{2}-\d{2})""")
private val RECURRENCE = Regex("""(?<!\S)@(daily|weekly|monthly)(?![\w-])""", RegexOption.IGNORE_CASE)
private val CREATED    = Regex("""@created:(\d{4}-\d{2}-\d{2})""")
private val ESTIMATE   = Regex("""~(?:\d+h(?:\d+m)?|\d+m)""")
private val MENTION    = Regex("""(?<!\S)@([A-Z][\w-]*)""")

private fun dateOf(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

private fun isDone(parts: ItemParts) = parts.check.equals("[x]", ignoreCase = true)

/** The date an item was stamped with @created:, if it has one */
internal fun createdOn(parts: ItemParts): LocalDate? = CREATED.find(parts.body)?.let { dateOf(it.groupValues[1]) }

/** An item's words without its @created: stamp */
internal fun withoutCreated(body: String): String = tidied(CREATED.replace(body, ""))

/** `daily`, `weekly` or `monthly` when the item's words recur, or null */
internal fun recurrenceOf(parts: ItemParts): String? = RECURRENCE.find(parts.body)?.groupValues?.get(1)?.lowercase()

private fun progress(done: Int, total: Int) = "$done/$total done  (${(done * 100.0 / total).roundToInt()}%)"

/** Pure: every item with a due date, soonest first; [today] marks the overdue ones */
fun upcomingDueDates(lines: List<String>, listPrefix: String, today: LocalDate): List<ReportEntry> =
    foundItems(lines, listPrefix)
        .mapNotNull { item ->
            val match = ITEM_DUE_DATE.find(item.parts.body) ?: return@mapNotNull null
            dateOf(match.groupValues[1])?.let { Triple(it, match, item) }
        }
        .sortedBy { (date, _, _) -> date }
        .map { (date, match, item) ->
            val status = if (date.isBefore(today)) " $OVERDUE" else ""
            ReportEntry(item.line, "$date$status - ${tidied(item.parts.body.replaceFirst(match.value, ""))}${item.suffix}")
        }

/** Pure: every item that recurs */
fun recurringItems(lines: List<String>, listPrefix: String): List<ReportEntry> =
    foundItems(lines, listPrefix).mapNotNull { item ->
        val match = RECURRENCE.find(item.parts.body) ?: return@mapNotNull null
        val text  = tidied(item.parts.body.replaceFirst(match.value, ""))
        ReportEntry(item.line, "@${match.groupValues[1].lowercase()} - $text${item.suffix}")
    }

/** Pure: the items created 30 or more days before [today], oldest first */
fun oldItems(lines: List<String>, listPrefix: String, today: LocalDate): List<ReportEntry> =
    foundItems(lines, listPrefix)
        .mapNotNull { item ->
            val match = CREATED.find(item.parts.body) ?: return@mapNotNull null
            val date  = dateOf(match.groupValues[1]) ?: return@mapNotNull null
            val age   = ChronoUnit.DAYS.between(date, today)
            if (age >= OLD_AFTER_DAYS) Triple(age, date, item) else null
        }
        .sortedByDescending { (age, _, _) -> age }
        .map { (age, date, item) ->
            ReportEntry(item.line, "${tidied(CREATED.replace(item.parts.body, ""))}${item.suffix} - $age days old ($date)")
        }

private fun estimated(lines: List<String>, listPrefix: String) =
    foundItems(lines, listPrefix)
        .mapNotNull { item -> itemEstimateMinutes(item.content)?.let { it to item } }
        .sortedByDescending { (minutes, _) -> minutes }

/** Pure: every item with a time estimate, longest first */
fun timeEstimates(lines: List<String>, listPrefix: String): List<ReportEntry> =
    estimated(lines, listPrefix).map { (_, item) ->
        val text = tidied(ESTIMATE.replaceFirst(item.parts.body, ""))
        ReportEntry(item.line, "~${estimateOf(item.content)} - $text${item.suffix}")
    }

/** Pure: how many items have estimates and what they add up to */
fun timeEstimatesTitle(lines: List<String>, listPrefix: String): String {
    val found = estimated(lines, listPrefix)
    return "${plural(found.size, "estimated item")} - total: ${formatTotal(found.sumOf { it.first })}"
}

/** Pure: the items, words, done count and tags of the caret's section; null outside a section */
fun sectionSummary(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val range  = header + 1..sectionLastLine(lines, header)
    val items  = foundItems(lines, listPrefix).filter { it.line in range }
    val done   = items.count { isDone(it.parts) }
    val words  = items.sumOf { itemWordCount(it.content) }
    val tags   = tagHits(lines, listPrefix).filter { it.line in range }.groupingBy { it.tag }.eachCount()
    val doneText = if (done > 0) "  Done: $done/${items.size}" else ""
    val tagText  = if (tags.isEmpty()) "" else "  Tags: " + tags.entries.joinToString(", ") { (tag, n) -> "#$tag×$n" }
    return "CL: \"${headerName(lines, header)}\" - ${plural(items.size, "item")}, ${plural(words, "word")}$doneText$tagText"
}

private fun hitsByTag(lines: List<String>, listPrefix: String) =
    tagHits(lines, listPrefix).groupBy { it.tag }.entries.sortedByDescending { it.value.size }

/** Pure: each tag with how many items carry it, most used first; choosing one goes to its first item */
fun itemCountsByTag(lines: List<String>, listPrefix: String): List<ReportEntry> =
    hitsByTag(lines, listPrefix).map { (tag, hits) -> ReportEntry(hits.first().line, "#$tag  ${plural(hits.size, "item")}") }

/** Pure: each tag with how much of it is done, most used first */
fun tagStats(lines: List<String>, listPrefix: String): List<ReportEntry> =
    hitsByTag(lines, listPrefix).map { (tag, hits) ->
        val done = hits.count { isDone(splitItem(it.itemText)) }
        ReportEntry(hits.first().line, "#$tag  ${progress(done, hits.size)}")
    }

internal fun mentionsIn(parts: ItemParts) = MENTION.findAll(parts.body).map { it.groupValues[1] }.distinct().toList()

private fun itemsByPerson(lines: List<String>, listPrefix: String): Map<String, List<FoundItem>> =
    foundItems(lines, listPrefix)
        .flatMap { item -> mentionsIn(item.parts).map { it to item } }
        .groupBy({ it.first }, { it.second })

/** Pure: each person mentioned with how many items name them, alphabetically */
fun mentionCounts(lines: List<String>, listPrefix: String): List<Pair<String, Int>> =
    itemsByPerson(lines, listPrefix).map { (name, items) -> name to items.size }.sortedBy { it.first }

/** Pure: the items that mention [name] */
fun itemsMentioning(lines: List<String>, listPrefix: String, name: String): List<ReportEntry> =
    itemsByPerson(lines, listPrefix)[name].orEmpty().map { ReportEntry(it.line, "${it.parts.body}${it.suffix}") }

/** Pure: each person mentioned with how much of their work is done, most items first */
fun mentionStats(lines: List<String>, listPrefix: String): List<ReportEntry> =
    itemsByPerson(lines, listPrefix).entries.sortedByDescending { it.value.size }.map { (name, items) ->
        ReportEntry(items.first().line, "@$name  ${progress(items.count { isDone(it.parts) }, items.size)}")
    }
