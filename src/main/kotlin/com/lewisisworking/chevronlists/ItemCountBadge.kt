/**
 * ItemCountBadge.kt
 * Pure logic for the item count badge: the number shown after a section
 * header when badges are enabled. Mirrors the VS Code extension, which
 * counts every bullet and numbered line in the section, nested lines
 * included. No IntelliJ Platform imports.
 */
package com.lewisisworking.chevronlists

/** Items in the section headed at [header]: every bullet or numbered line before the next header */
fun badgeItemLines(lines: List<String>, header: Int, listPrefix: String): List<Int> {
    val out = mutableListOf<Int>()
    for (i in header + 1 until lines.size) {
        if (isHeader(lines[i])) break
        if (parseBullet(lines[i], listPrefix) != null || parseNumbered(lines[i]) != null) out += i
    }
    return out
}

/** Badge text for the section headed at [header], e.g. "  (3)" */
fun badgeText(lines: List<String>, header: Int, listPrefix: String): String =
    "  (${badgeItemLines(lines, header, listPrefix).size})"
