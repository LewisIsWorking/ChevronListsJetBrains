/**
 * Bookmarks.kt
 * Pure logic for bookmark markers, `>> [bookmark:Name]` lines. No IntelliJ Platform
 * imports - fully testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

private val BOOKMARK = Regex("""^\s*>>\s*\[bookmark:([^\]]+)]""")

/** Pure: the bookmark name on [line], or null if it is not a bookmark */
fun parseBookmark(line: String): String? =
    BOOKMARK.find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

/** Pure: one entry per bookmark of the file */
fun bookmarkEntries(lines: List<String>): List<ReportEntry> =
    lines.indices.mapNotNull { i -> parseBookmark(lines[i])?.let { ReportEntry(i, it) } }

/** Pure: the marker line for a bookmark called [name], or null if nothing usable is left of it */
fun bookmarkMarker(name: String): String? =
    name.replace("]", "").trim().takeIf { it.isNotEmpty() }?.let { ">> [bookmark:$it]" }
