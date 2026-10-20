/**
 * SectionEdits.kt
 * Pure logic for duplicating or deleting the `> Header` section at the caret.
 * No IntelliJ Platform imports - fully testable with plain JUnit. Ported from
 * the VS Code extension's Duplicate Section and Delete Section.
 */
package com.lewisisworking.chevronlists

/** Lines to insert after [afterLine] */
data class LineInsert(val afterLine: Int, val lines: List<String>)

/** The header of the section holding [caretLine]: the line itself when it is a header */
private fun caretSectionHeader(lines: List<String>, caretLine: Int): Int? {
    if (caretLine !in lines.indices) return null
    return if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine)
}

/**
 * Pure: the lines to delete for the section holding [caretLine], or null outside
 * a section. That is the header, its content and the blank lines before the next
 * header. The last section takes the blank lines above it instead, so the file
 * keeps a single trailing blank line rather than ending in several.
 */
fun deleteSectionRange(lines: List<String>, caretLine: Int): IntRange? {
    val header = caretSectionHeader(lines, caretLine) ?: return null
    val next = findHeaderBelow(lines, header)
    if (next != null) return header until next
    var start = header
    while (start > 0 && lines[start - 1].isBlank()) start--
    return start..sectionContentEnd(lines, header)
}

/**
 * Pure: a copy of the section holding [caretLine], notes included, to insert
 * after its last non-blank line, or null outside a section. The copy is led by
 * the blank lines the section already has before whatever follows it.
 */
fun duplicateSection(lines: List<String>, caretLine: Int): LineInsert? {
    val header = caretSectionHeader(lines, caretLine) ?: return null
    val contentEnd = sectionContentEnd(lines, header)
    val next = findHeaderBelow(lines, header) ?: lines.size
    return LineInsert(contentEnd, lines.subList(contentEnd + 1, next) + lines.subList(header, contentEnd + 1))
}
