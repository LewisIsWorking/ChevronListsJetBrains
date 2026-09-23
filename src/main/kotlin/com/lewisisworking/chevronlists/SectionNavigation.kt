/**
 * SectionNavigation.kt
 * Pure logic for jumping between `> Header` lines. No IntelliJ Platform imports.
 * Mirrors findHeaderBelow / findHeaderAbove in the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** Pure: the first header strictly below [fromLine], or null if there is none */
fun findHeaderBelow(lines: List<String>, fromLine: Int): Int? =
    (maxOf(fromLine + 1, 0) until lines.size).firstOrNull { isHeader(lines[it]) }

/** Pure: the nearest header strictly above [fromLine], or null if there is none */
fun findHeaderAbove(lines: List<String>, fromLine: Int): Int? =
    (minOf(fromLine - 1, lines.size - 1) downTo 0).firstOrNull { isHeader(lines[it]) }
