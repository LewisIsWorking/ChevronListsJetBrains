/**
 * SectionFocus.kt
 * Pure logic for hidden sections: a `>> [hidden]` line inside a section. No IntelliJ
 * Platform imports - fully testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

internal const val HIDDEN_MARKER = ">> [hidden]"

private fun isHiddenMarker(line: String) = line.trim() == HIDDEN_MARKER

/** Pure: whether the section at [header] holds a hidden marker */
fun isSectionHidden(lines: List<String>, header: Int): Boolean =
    (header + 1..sectionLastLine(lines, header)).any { isHiddenMarker(lines[it]) }

/** Pure: the line of every hidden marker */
fun hiddenMarkerLines(lines: List<String>): List<Int> = lines.indices.filter { isHiddenMarker(lines[it]) }

/** Pure: whether the text of a section's fold holds a hidden marker */
fun foldHoldsHiddenMarker(text: String): Boolean = text.split("\n").any(::isHiddenMarker)
