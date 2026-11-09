/**
 * LockedSections.kt
 * Pure logic for section locks: a `>> [locked]` line inside a section. No IntelliJ
 * Platform imports - fully testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

internal const val LOCK_MARKER = ">> [locked]"

private fun lockMarkerLine(lines: List<String>, header: Int): Int? =
    (header + 1..sectionLastLine(lines, header)).firstOrNull { lines[it].trim() == LOCK_MARKER }

/** Pure: [lines] with the section at [header] locked; null if it already is */
fun withSectionLocked(lines: List<String>, header: Int): List<String>? =
    if (lockMarkerLine(lines, header) != null) null
    else lines.take(header + 1) + LOCK_MARKER + lines.drop(header + 1)

/** Pure: [lines] with the section at [header] unlocked; null if it is not locked */
fun withSectionUnlocked(lines: List<String>, header: Int): List<String>? {
    val marker = lockMarkerLine(lines, header) ?: return null
    return lines.filterIndexed { index, _ -> index != marker }
}

/** Pure: the sections locked both [before] and [after] a save whose text it changes */
fun changedLockedSections(before: List<String>, after: List<String>): List<String> =
    after.indices.filter { isHeader(after[it]) && lockMarkerLine(after, it) != null }.mapNotNull { header ->
        val name = plainSectionName(after, header)
        val old  = sectionNamed(before, name)?.takeIf { lockMarkerLine(before, it) != null } ?: return@mapNotNull null
        name.takeIf { sectionBlock(before, old) != sectionBlock(after, header) }
    }.distinct()
