/**
 * SectionInserts.kt
 * Pure logic for adding a new section, or a "(copy)" of the caret section. No
 * IntelliJ Platform imports - fully testable with plain JUnit. Ported from the
 * VS Code extension's New Section and Clone Section.
 */
package com.lewisisworking.chevronlists

/** [lines] to insert before line [beforeLine] (which may be the line count), and the line the header lands on */
data class SectionBlockInsert(val beforeLine: Int, val lines: List<String>, val headerLine: Int)

/**
 * Pure: where a new section called [name] goes for a caret on [caretLine]. On a
 * header, or outside any section, it goes at the caret with a blank line after
 * it. Inside a section it goes after that section's content, so the items below
 * the caret are not handed to the new section.
 */
fun newSectionInsert(lines: List<String>, caretLine: Int, name: String, listPrefix: String): SectionBlockInsert {
    val block = listOf("> ${name.trim()}", ">> $listPrefix ")
    val header = if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine)
    if (header == null || header == caretLine) {
        val gap = if (lines[caretLine].isBlank()) emptyList() else listOf("")
        return SectionBlockInsert(caretLine, block + gap, caretLine)
    }
    val end = sectionContentEnd(lines, header)
    return SectionBlockInsert(end + 1, listOf("") + block, end + 2)
}

/** Pure: a "(copy)" of the caret section after its content and a blank line, or null outside a section */
fun cloneSectionInsert(lines: List<String>, caretLine: Int): SectionBlockInsert? {
    if (caretLine !in lines.indices) return null
    val header = if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine) ?: return null
    val end = sectionContentEnd(lines, header)
    val copy = listOf("", "> ${parseHeader(lines[header])!!.content.trim()} (copy)") + lines.subList(header + 1, end + 1)
    return SectionBlockInsert(end + 1, copy, end + 2)
}
