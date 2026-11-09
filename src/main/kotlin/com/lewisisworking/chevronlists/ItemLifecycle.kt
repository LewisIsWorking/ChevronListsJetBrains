/**
 * ItemLifecycle.kt
 * Pure reports and edits about how old items are and what comes after them:
 * age reports, word goals, complexity, multi-tag filters, recurrence, grouping,
 * collecting, removing and archiving.
 * No IntelliJ imports. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private const val AGE_REPORT_LIMIT = 50
private const val BAR_CELLS        = 10
private const val COMPLEXITY_FULL  = 7
private const val STATS_TEXT_CHARS = 40
private const val UNASSIGNED       = "Unassigned"

private val TAG_SPLIT   = Regex("""[\s,]+""")
private val DIVIDER     = Regex("""^>> -- .+$""")
private val LABEL       = Regex("""\[[A-Z][^\]]*]""")
private val EXPIRES     = Regex("""@expires:\d{4}-\d{2}-\d{2}""")

/** The recurring items Insert Recurring Item offers: name, and the words of the item */
val RECURRING_TEMPLATES = listOf(
    "Daily Standup"   to "[ ] Daily standup @daily",
    "Weekly Review"   to "[ ] Weekly review @weekly",
    "Monthly Report"  to "[ ] Monthly report @monthly",
    "Daily Check-in"  to "[ ] Check in @daily",
    "Weekly Planning" to "[ ] Weekly planning @weekly",
    "Monthly Goals"   to "[ ] Review monthly goals @monthly",
)

private fun bar(percent: Int): String {
    val filled = (percent / 10.0).roundToInt()
    return "▓".repeat(filled) + "░".repeat(BAR_CELLS - filled)
}

private fun days(n: Long) = if (n == 1L) "1 day" else "$n days"

private class AgedItem(val age: Long, val date: LocalDate, val item: FoundItem) {
    val words get() = withoutCreated(item.parts.body)
}

private fun agedItems(lines: List<String>, listPrefix: String, today: LocalDate): List<AgedItem> =
    foundItems(lines, listPrefix)
        .mapNotNull { item -> createdOn(item.parts)?.let { AgedItem(ChronoUnit.DAYS.between(it, today), it, item) } }
        .sortedByDescending { it.age }

/** Pure: the oldest stamped items in the file, oldest first */
fun itemAgeReport(lines: List<String>, listPrefix: String, today: LocalDate): List<ReportEntry> =
    agedItems(lines, listPrefix, today).take(AGE_REPORT_LIMIT).map {
        ReportEntry(it.item.line, "${days(it.age)} - ${it.words}${it.item.suffix}")
    }

/** Pure: the title of the age report, saying how much of the file it shows */
fun itemAgeReportTitle(lines: List<String>, listPrefix: String, today: LocalDate): String {
    val total = agedItems(lines, listPrefix, today).size
    return "Item Age Report (oldest ${minOf(total, AGE_REPORT_LIMIT)} of $total)"
}

/** Pure: how old the stamped items of the caret's section are; null outside a section */
fun ageStatsMessage(lines: List<String>, caretLine: Int, listPrefix: String, today: LocalDate): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val name   = headerName(lines, header)
    val range  = header + 1..sectionLastLine(lines, header)
    val aged   = agedItems(lines, listPrefix, today).filter { it.item.line in range }
    if (aged.isEmpty()) return "CL: \"$name\" - no @created: stamps found. Use CL: Stamp Item to add them."
    val oldest  = aged.first()
    val newest  = aged.last()
    val average = aged.map { it.age }.average().roundToInt().toLong()
    return "CL: \"$name\" - ${plural(aged.size, "stamped item")}" +
        "  Oldest: ${oldest.date} (${oldest.age}d) ${oldest.words.take(STATS_TEXT_CHARS)}" +
        "  Newest: ${newest.date} (${newest.age}d) ${newest.words.take(STATS_TEXT_CHARS)}" +
        "  Average age: ${days(average)}"
}

/** Pure: each section with a ==N goal and its progress, furthest from its goal first */
fun wordCountGoals(lines: List<String>, listPrefix: String): List<ReportEntry> =
    lines.indices.mapNotNull { header ->
        val goal = parseHeader(lines[header])?.let { sectionGoal(it.content) } ?: return@mapNotNull null
        val range = header + 1..sectionLastLine(lines, header)
        Triple(header, foundItems(lines, listPrefix).filter { it.line in range }.sumOf { itemWordCount(it.content) }, goal)
    }.sortedByDescending { (_, words, goal) -> maxOf(0, goal - words) }.map { (header, words, goal) ->
        val percent = if (goal == 0) 100 else minOf(100, (words * 100.0 / goal).roundToInt())
        val icon    = if (percent >= 100) "✅" else if (percent >= 50) "◐" else "○"
        val detail  = if (words < goal) "${goal - words} words to go" else "${words - goal} over goal"
        ReportEntry(header, "$icon ${headerName(lines, header)}  ${bar(percent)} $words/$goal ($percent%) - $detail")
    }

/** Pure: the marker score of the item on [line]; null unless the line is an item */
fun itemComplexityMessage(line: String, listPrefix: String): String? {
    val content  = itemContent(line, listPrefix) ?: return null
    val parts    = splitItem(content)
    val priority = parts.priority.length
    val tags     = findTags(line).map { line.substring(it.first + 1, it.last + 1).lowercase() }.distinct().size
    val estimate = if (itemEstimateMinutes(content) != null) 1 else 0
    val due      = if (ITEM_DUE_DATE.containsMatchIn(parts.body)) 1 else 0
    val expiry   = if (EXPIRES.containsMatchIn(parts.body)) 1 else 0
    val vote     = if (parts.vote != null) 1 else 0
    val label    = if (LABEL.containsMatchIn(parts.body)) 1 else 0
    val total    = priority + tags + estimate + due + expiry + vote + label
    val percent  = minOf(100, (total * 100.0 / COMPLEXITY_FULL).roundToInt())
    return "CL: Complexity ${bar(percent)} $percent%  Priority: $priority  Tags: $tags  Estimate: $estimate" +
        "  Due: $due  Expiry: $expiry  Vote: $vote  Label: $label"
}

/** Pure: tag names from what was typed, such as `#a, b` */
fun parseTagList(input: String): List<String> =
    TAG_SPLIT.split(input.trim()).map { it.removePrefix("#").lowercase() }.filter { it.isNotEmpty() }.distinct()

/** Pure: the items with all of [tags], or with any of them */
fun itemsWithTags(lines: List<String>, listPrefix: String, tags: List<String>, matchAll: Boolean): List<ReportEntry> {
    val tagsByLine = tagHits(lines, listPrefix).groupBy({ it.line }, { it.tag })
    return foundItems(lines, listPrefix).filter { item ->
        val own = tagsByLine[item.line].orEmpty()
        if (matchAll) own.containsAll(tags) else tags.any { it in own }
    }.map { ReportEntry(it.line, "${it.parts.body}${it.suffix}") }
}

/** Pure: the title of a multi-tag list, such as `2 items matching #a & #b` */
fun tagFilterTitle(count: Int, tags: List<String>, matchAll: Boolean): String =
    "${plural(count, "item")} matching ${tags.joinToString(if (matchAll) " & " else " | ") { "#$it" }}"

/** Pure: the next occurrence of the recurring item [line]; null unless it recurs. A done item comes back open. */
fun nextOccurrenceLine(line: String, listPrefix: String, today: LocalDate): String? {
    val content = itemContent(line, listPrefix) ?: return null
    val parts   = splitItem(content)
    val type    = recurrenceOf(parts) ?: return null
    val due     = ITEM_DUE_DATE.find(parts.body)
    val base    = due?.let { runCatching { LocalDate.parse(it.groupValues[1]) }.getOrNull() } ?: today
    val next    = when (type) {
        "daily"  -> base.plusDays(1)
        "weekly" -> base.plusWeeks(1)
        else     -> base.plusMonths(1)
    }
    val words  = tidied(if (due != null) parts.body.replaceFirst(due.value, "") else parts.body)
    val copied = joinItem(parts.copy(check = if (parts.check.isEmpty()) "" else "[ ]", body = "$words @$next"))
    val numbered = parseNumbered(line)
    return if (numbered != null) "${numbered.chevrons} ${numbered.num + 1}. $copied"
    else "${parseBullet(line, listPrefix)!!.chevrons} ${parseBullet(line, listPrefix)!!.prefix} $copied"
}

/** The result of grouping or collecting: the new document, and how many groups or items it made */
class LinesResult(val lines: List<String>, val count: Int)

/**
 * Pure: the caret section's items under a `>> -- Name` divider for the person each
 * mentions first. Other lines stay, after the groups, and a divider from an
 * earlier run is replaced. Null outside a section or when nobody is mentioned.
 */
fun groupItemsByMention(lines: List<String>, caretLine: Int, listPrefix: String): LinesResult? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val last   = sectionLastLine(lines, header)
    val groups = linkedMapOf<String, MutableList<String>>()
    val others = mutableListOf<String>()
    val spare  = mutableListOf<String>()
    for (i in header + 1..last) {
        val content = itemContent(lines[i], listPrefix)
        when {
            content != null -> {
                val person = mentionsIn(splitItem(content)).firstOrNull()
                (if (person != null) groups.getOrPut(person) { mutableListOf() } else spare).add(lines[i])
            }
            !DIVIDER.matches(lines[i]) -> others += lines[i]
        }
    }
    if (groups.isEmpty()) return null
    if (spare.isNotEmpty()) groups[UNASSIGNED] = spare
    val grouped = groups.flatMap { (name, items) -> listOf(">> -- $name") + items }
    return LinesResult(lines.subList(0, header + 1) + grouped + others + lines.subList(last + 1, lines.size), groups.size)
}

/** Pure: copies of every item tagged [tag] in a new `> #tag Results` section at the end; null when none. An earlier results section is not collected again. */
fun collectItemsByTag(lines: List<String>, listPrefix: String, tag: String): LinesResult? {
    val results = "#$tag Results"
    val hits    = tagHits(lines, listPrefix).filter { it.tag == tag && it.section != results }
    if (hits.isEmpty()) return null
    val kept      = lines.dropLastWhile { it.isBlank() }
    val separator = if (kept.isEmpty()) emptyList() else listOf("")
    return LinesResult(kept + separator + "> $results" + hits.map { lines[it.line] } + "", hits.size)
}

/** Pure: the document without items created at least [days] days before [today]; null when none */
fun removeOldItems(lines: List<String>, listPrefix: String, days: Int, today: LocalDate): LinesResult? {
    val old = agedItems(lines, listPrefix, today).filter { it.age >= days }.map { it.item.line }.toSet()
    if (old.isEmpty()) return null
    return LinesResult(lines.filterIndexed { i, _ -> i !in old }, old.size)
}

/** Pure: done items created at least [days] days before [today], moved to the archive with anything nested under them; null when none */
fun archiveOldDoneItems(lines: List<String>, listPrefix: String, days: Int, today: LocalDate): ArchiveResult? {
    val archive = lines.indexOfFirst { isArchiveHeader(it) }
    val kept    = if (archive >= 0) archive..sectionLastLine(lines, archive) else IntRange.EMPTY
    val old = agedItems(lines, listPrefix, today)
        .filter { it.age >= days && it.item.line !in kept && it.item.parts.check.equals("[x]", ignoreCase = true) }
        .map { it.item.line }
    return moveBlocksToArchive(lines, old.sorted(), listPrefix)
}
