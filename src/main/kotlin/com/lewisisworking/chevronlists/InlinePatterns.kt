/**
 * InlinePatterns.kt
 * Pure detection of inline tokens within a line's text content - tags, priority
 * markers, and due dates. No IntelliJ Platform imports - fully testable with
 * plain JUnit. Returns text ranges so the annotator can highlight them at the
 * right offsets.
 */
package com.lewisisworking.chevronlists

/** A priority marker found inside a line, with its range and severity (1, 2 or 3) */
data class PriorityMatch(val range: IntRange, val level: Int)

// Tag:      after whitespace or start, "#" then alpha first char then word chars and dashes
private val TAG_REGEX      = Regex("""(?:^|\s)(#[A-Za-z][\w-]*)""")
// Priority: standalone token, 1-3 exclamation marks, surrounded by whitespace or string bounds
private val PRIORITY_REGEX = Regex("""(?:^|\s)(!{1,3})(?=\s|$)""")
// Due date: after whitespace or start, "@YYYY-MM-DD" (ISO only for v0.9)
private val DUE_DATE_REGEX = Regex("""(?:^|\s)(@\d{4}-\d{2}-\d{2})""")

/**
 * Pure: returns the text ranges (in line-local coordinates) of all `#tag` tokens.
 * Ranges are inclusive on both ends, matching Kotlin Regex convention.
 */
fun findTags(line: String): List<IntRange> =
    TAG_REGEX.findAll(line).mapNotNull { it.groups[1]?.range }.toList()

/**
 * Pure: returns priority markers (`!`, `!!`, `!!!`) found as standalone tokens,
 * each annotated with its level (1, 2 or 3 = exclamation count).
 */
fun findPriorities(line: String): List<PriorityMatch> =
    PRIORITY_REGEX.findAll(line).mapNotNull { match ->
        val g = match.groups[1] ?: return@mapNotNull null
        PriorityMatch(g.range, g.value.length)
    }.toList()

/**
 * Pure: returns the text ranges of `@YYYY-MM-DD` due-date markers.
 */
fun findDueDates(line: String): List<IntRange> =
    DUE_DATE_REGEX.findAll(line).mapNotNull { it.groups[1]?.range }.toList()