/**
 * SectionInsights.kt
 * Pure section rankings and checks, pinning a section to the top, and working out
 * an =expression in an item.
 * No IntelliJ imports. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val SIMILAR_FROM   = 0.7
private const val LONG_ITEM      = 200
private const val ITEM_WEIGHT    = 3
private const val PLACES         = 10000.0

private val EXPRESSION = Regex("""=([0-9+\-*/.() ]+)""")

private fun headerLines(lines: List<String>) = lines.indices.filter { isHeader(lines[it]) }

private fun isChecked(parts: ItemParts) = parts.check.equals("[x]", ignoreCase = true)

/** Items of the file, each kept under the header line of its section */
private fun itemsBySection(lines: List<String>, listPrefix: String): Map<Int, List<FoundItem>> {
    val items = foundItems(lines, listPrefix)
    return headerLines(lines).associateWith { header ->
        val range = header + 1..sectionLastLine(lines, header)
        items.filter { it.line in range }
    }
}

/** Pure: every section with its weight, items x 3 + priorities + votes + tags, heaviest first */
fun sectionWeights(lines: List<String>, listPrefix: String): List<ReportEntry> {
    val tagsOnLine = tagHits(lines, listPrefix).groupingBy { it.line }.eachCount()
    return itemsBySection(lines, listPrefix).map { (header, items) ->
        val weight = items.size * ITEM_WEIGHT +
            items.sumOf { it.parts.priority.length + (it.parts.vote ?: 0) + (tagsOnLine[it.line] ?: 0) }
        weight to ReportEntry(header, "${headerName(lines, header)}  weight: $weight")
    }.sortedByDescending { it.first }.map { it.second }
}

/** Pure: the sections with checkboxes, all of them done */
fun completedSections(lines: List<String>, listPrefix: String): List<ReportEntry> =
    itemsBySection(lines, listPrefix).mapNotNull { (header, items) ->
        val boxed = items.filter { it.parts.check.isNotEmpty() }
        if (boxed.isEmpty() || !boxed.all { isChecked(it.parts) }) return@mapNotNull null
        ReportEntry(header, "✅ ${headerName(lines, header)}  ${plural(boxed.size, "item")} - all done")
    }

/** Pure: empty, very long and repeated items in the caret's section; null outside a section */
fun sectionHealthIssues(lines: List<String>, caretLine: Int, listPrefix: String): List<ReportEntry>? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val seen   = mutableMapOf<String, Int>()
    val issues = mutableListOf<ReportEntry>()
    for (i in header + 1..sectionLastLine(lines, header)) {
        val plain = stripItemMetadata(itemContent(lines[i], listPrefix) ?: continue).trim()
        if (plain.isEmpty()) {
            issues += ReportEntry(i, "Line ${i + 1} - Item has no content after stripping metadata")
            continue
        }
        if (plain.length > LONG_ITEM) issues += ReportEntry(i, "Line ${i + 1} - Item is very long (${plain.length} chars) - consider splitting")
        val first = seen.putIfAbsent(plain.lowercase(), i)
        if (first != null) issues += ReportEntry(i, "Line ${i + 1} - Duplicate item content (same as line ${first + 1})")
    }
    return issues
}

/** Pure: the document with the caret's section first; null outside a section or when it is first already */
fun pinSectionToTop(lines: List<String>, caretLine: Int): List<String>? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val first  = lines.indexOfFirst { isHeader(it) }
    if (first == header) return null
    val last = sectionLastLine(lines, header)
    // The last section of a file has no blank line of its own, so it takes the one before it
    val borrows = last == lines.lastIndex && lines[last].isNotBlank() && lines[header - 1].isBlank()
    val moved   = lines.subList(header, last + 1) + if (borrows) listOf("") else emptyList()
    val between = lines.subList(first, if (borrows) header - 1 else header)
    return lines.subList(0, first) + moved + between + lines.subList(last + 1, lines.size)
}

private fun levenshtein(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val current = IntArray(b.length + 1)
        current[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
        }
        previous = current
    }
    return previous[b.length]
}

private fun similarity(a: String, b: String) =
    1 - levenshtein(a.lowercase(), b.lowercase()).toDouble() / max(max(a.length, b.length), 1)

/** Pure: pairs of sections with close but not equal names, in file order */
fun similarSections(lines: List<String>): List<ReportEntry> {
    val headers = headerLines(lines).map { it to parseHeader(lines[it])!!.content.trim() }
    return headers.flatMapIndexed { i, (line, name) ->
        headers.drop(i + 1).mapNotNull { (_, other) ->
            val score = similarity(name, other)
            if (score < SIMILAR_FROM || score >= 1) null
            else ReportEntry(line, "\"$name\" ↔ \"$other\"  ${(score * 100).roundToInt()}% similar")
        }
    }
}

/** Pure: one entry per section with its items, done count, flags, overdue items and word goal; [today] is an ISO date */
fun progressReport(lines: List<String>, listPrefix: String, today: String): List<ReportEntry> =
    itemsBySection(lines, listPrefix).map { (header, items) ->
        val boxed   = items.filter { it.parts.check.isNotEmpty() }
        val done    = boxed.count { isChecked(it.parts) }
        val flagged = items.count { isFlagged(it.parts) }
        val overdue = items.count { item ->
            !isChecked(item.parts) && ITEM_DUE_DATE.find(item.parts.body)?.groupValues?.get(1)?.let { it < today } == true
        }
        val words = items.sumOf { itemWordCount(it.content) }
        val goal  = sectionGoal(parseHeader(lines[header])!!.content)
        val parts = listOfNotNull(
            plural(items.size, "item"),
            if (boxed.isNotEmpty()) "$done/${boxed.size} done (${(done * 100.0 / boxed.size).roundToInt()}%)" else null,
            if (flagged > 0) "$flagged flagged" else null,
            if (overdue > 0) "⚠ $overdue overdue" else null,
            goal?.let { "$words/$it words (${if (it == 0) 100 else minOf(100, (words * 100.0 / it).roundToInt())}%)" },
        )
        ReportEntry(header, "${headerName(lines, header)}: ${parts.joinToString(" · ")}")
    }

/** Numbers, + - * / ** and brackets, read left to right */
private class Arithmetic(private val text: String) {
    private var at = 0

    private fun skipSpaces() { while (at < text.length && text[at] == ' ') at++ }

    private fun take(symbol: String): Boolean {
        skipSpaces()
        if (!text.startsWith(symbol, at)) return false
        at += symbol.length
        return true
    }

    /** The value of the whole text, or null unless all of it is arithmetic */
    fun value(): Double? {
        val result = sum() ?: return null
        skipSpaces()
        return if (at == text.length) result else null
    }

    private fun sum(): Double? {
        var total = product() ?: return null
        while (true) {
            total = when {
                take("+") -> total + (product() ?: return null)
                take("-") -> total - (product() ?: return null)
                else      -> return total
            }
        }
    }

    private fun product(): Double? {
        var total = power() ?: return null
        while (true) {
            total = when {
                take("*") -> total * (power() ?: return null)
                take("/") -> total / (power() ?: return null)
                else      -> return total
            }
        }
    }

    private fun power(): Double? {
        val base = unary() ?: return null
        return if (take("**")) base.pow(power() ?: return null) else base
    }

    private fun unary(): Double? = when {
        take("-")  -> unary()?.let { -it }
        take("+")  -> unary()
        take("(")  -> sum()?.takeIf { take(")") }
        else       -> number()
    }

    private fun number(): Double? {
        skipSpaces()
        val start = at
        while (at < text.length && (text[at].isDigit() || text[at] == '.')) at++
        return text.substring(start, at).toDoubleOrNull()
    }
}

/** What an =expression came to */
class EvaluatedItem(val line: String, val expression: String, val result: String)

/** Pure: the item on [line] with the first =expression in its words replaced by its value; null without a valid one */
fun evaluateItemExpression(line: String, listPrefix: String): EvaluatedItem? {
    val parts = splitItem(itemContent(line, listPrefix) ?: return null)
    val match = EXPRESSION.find(parts.body) ?: return null
    val value = Arithmetic(match.groupValues[1]).value()?.takeIf { it.isFinite() } ?: return null
    val shown = BigDecimal((value * PLACES).roundToLong() / PLACES).setScale(4, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString()
    val written = match.value.trimEnd()
    val after = transformItemLine(line, listPrefix) { joinItem(parts.copy(body = parts.body.replaceFirst(written, "=$shown"))) }!!
    return EvaluatedItem(after, written.drop(1), shown)
}
