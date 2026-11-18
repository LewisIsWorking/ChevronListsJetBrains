/**
 * PinnedSections.kt
 * Pure logic for pinned sections: section names kept in a set, matched
 * case-insensitively, toggled by name. No IntelliJ Platform imports - fully
 * testable with plain JUnit. Ported from the VS Code extension's pinState.ts.
 */
package com.lewisisworking.chevronlists

/** One pinned section: its header line and display name */
data class PinnedSection(val header: Int, val name: String)

/** Pure: the key a section name is stored under - pins match case-insensitively */
fun pinKey(name: String): String = name.lowercase()

/** Pure: [pins] with [name] added when absent, removed when present */
fun togglePinned(pins: Set<String>, name: String): Set<String> {
    val key = pinKey(name)
    return if (key in pins) pins - key else pins + key
}

/** Pure: the (header, name) of every section whose name is in [pins] */
fun pinnedSections(lines: List<String>, pins: Set<String>): List<PinnedSection> {
    if (pins.isEmpty()) return emptyList()
    return lines.indices.mapNotNull { h ->
        if (!isHeader(lines[h])) return@mapNotNull null
        val name = parseHeader(lines[h])!!.content.trim()
        if (pinKey(name) in pins) PinnedSection(h, name) else null
    }
}
