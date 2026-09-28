/**
 * SectionEntries.kt
 * Pure logic for Go to Section: every `> Header` in a document, with its line
 * and item count. No IntelliJ Platform imports.
 */
package com.lewisisworking.chevronlists

/** A section as listed by Go to Section */
data class SectionEntry(val line: Int, val title: String, val items: Int) {
    /** What the popup shows, e.g. "Groceries  (12 items)" */
    val label: String get() = when (items) {
        0    -> title
        1    -> "$title  (1 item)"
        else -> "$title  ($items items)"
    }
}

/** Pure: every section in document order, including empty ones */
fun sectionEntries(lines: List<String>, listPrefix: String): List<SectionEntry> {
    val entries = mutableListOf<SectionEntry>()
    var line = -1
    var title = ""
    var items = 0
    for ((i, text) in lines.withIndex()) {
        val header = parseHeader(text)
        if (header != null) {
            if (line >= 0) entries += SectionEntry(line, title, items)
            line = i; title = header.content.trim(); items = 0
            continue
        }
        if (line >= 0 && (parseNumbered(text) != null || parseBullet(text, listPrefix) != null)) items++
    }
    if (line >= 0) entries += SectionEntry(line, title, items)
    return entries
}
