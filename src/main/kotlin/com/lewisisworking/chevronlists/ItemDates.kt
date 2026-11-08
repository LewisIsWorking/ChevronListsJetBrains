/**
 * ItemDates.kt
 * Pure logic for natural dates, due dates, Fix Numbering and finding a section's
 * items. No IntelliJ Platform imports - fully testable with plain JUnit. Ported
 * from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate

private val RELATIVE_DAYS = Regex("""^\+(\d+)$""")
private val ISO_DATE      = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val DUE_DATE_MARK = Regex("""@\d{4}-\d{2}-\d{2}""")

private val WEEKDAYS = DayOfWeek.values().flatMap { day ->
    val name = day.name.lowercase()
    listOf(name to day, name.take(3) to day)
}.toMap()

/**
 * Pure: [input] as YYYY-MM-DD: an ISO date, a weekday (the next one, a week on
 * when it is today), +N days, today, tomorrow, next week or next month. Null when
 * it is not understood or not a real date.
 */
fun parseNaturalDate(input: String, today: LocalDate): String? {
    val text = input.trim().lowercase()
    RELATIVE_DAYS.find(text)?.let { match ->
        val days = match.groupValues[1].toLongOrNull() ?: return null
        return try { today.plusDays(days).toString() } catch (_: DateTimeException) { null }
    }
    if (ISO_DATE.matches(text)) {
        return try { LocalDate.parse(text).toString() } catch (_: DateTimeException) { null }
    }
    WEEKDAYS[text]?.let { day ->
        val ahead = (day.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(if (ahead == 0) 7L else ahead.toLong()).toString()
    }
    return when (text) {
        "today"      -> today
        "tomorrow"   -> today.plusDays(1)
        "next week"  -> today.plusDays(7)
        "next month" -> today.plusMonths(1)
        else         -> null
    }?.toString()
}

/** Pure: [content] due on [date]: replaces any due date, at the end of the words, before a vote or comment */
fun setItemDueDate(content: String, date: String): String {
    val parts = splitItem(content)
    return joinItem(parts.copy(body = "${parts.body.replaceFirst(DUE_DATE_MARK, "")} @$date"))
}

/** Pure: every list from the first section down numbered from 1, each list counted separately */
fun fixNumbering(lines: List<String>): List<String> {
    val start = lines.indexOfFirst(::isHeader)
    if (start < 0) return lines
    return lines.subList(0, start) + renumberLines(lines.subList(start, lines.size))
}

/** Pure: the item lines of the section at [caretLine], empty outside a section */
fun sectionItemLines(lines: List<String>, caretLine: Int, listPrefix: String): List<Int> {
    if (caretLine !in lines.indices) return emptyList()
    val header = if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine) ?: return emptyList()
    val end = findHeaderBelow(lines, header) ?: lines.size
    return (header + 1 until end).filter { parseBullet(lines[it], listPrefix) != null || parseNumbered(lines[it]) != null }
}
