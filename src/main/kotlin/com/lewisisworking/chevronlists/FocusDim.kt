/**
 * FocusDim.kt
 * Pure logic for focus-mode dimming: the line ranges of every section except
 * the one holding the caret. No IntelliJ Platform imports - fully testable
 * with plain JUnit. Ported from the VS Code extension's dimDecoration.ts.
 */
package com.lewisisworking.chevronlists

/**
 * Pure: the line range of every section except the caret's section, dimmed
 * while focus mode is on. Empty when the caret sits in no section, or when
 * the file holds a single section.
 */
fun dimmedRanges(lines: List<String>, caretLine: Int): List<IntRange> {
    if (caretLine !in lines.indices) return emptyList()
    val home = sectionHeaderLine(lines, caretLine) ?: return emptyList()
    return lines.indices
        .filter { isHeader(lines[it]) && it != home }
        .map { header -> header..sectionLastLine(lines, header) }
}
