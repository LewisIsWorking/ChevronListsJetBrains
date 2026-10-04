/**
 * TagIndex.kt
 * Pure logic for Filter by Tag: which #tags the file's items carry, and where.
 * No IntelliJ Platform imports. Tags follow this plugin's rule (findTags in
 * InlinePatterns.kt: a letter first, so "#123" is not a tag) and are compared
 * case-insensitively. Only items are indexed, as in the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** One item carrying a tag: the item's line, its text after the marker, and its section */
data class TagHit(val tag: String, val line: Int, val itemText: String, val section: String) {
    /** What the item list shows, e.g. "write notes #urgent  (Work)" */
    val label: String get() = if (section.isEmpty()) itemText else "$itemText  ($section)"
}

/** Pure: every (item, tag) pair in document order; an item lists each of its tags once */
fun tagHits(lines: List<String>, listPrefix: String): List<TagHit> {
    val hits = mutableListOf<TagHit>()
    var section = ""
    for ((i, text) in lines.withIndex()) {
        val header = parseHeader(text)
        if (header != null) { section = header.content.trim(); continue }
        val content = parseNumbered(text)?.content ?: parseBullet(text, listPrefix)?.content ?: continue
        val tags = findTags(text).map { text.substring(it.first + 1, it.last + 1).lowercase() }.distinct()
        for (tag in tags) hits += TagHit(tag, i, content.trim(), section)
    }
    return hits
}

/** Pure: each tag with how many items carry it, alphabetically */
fun tagCounts(hits: List<TagHit>): List<Pair<String, Int>> =
    hits.groupingBy { it.tag }.eachCount().toList().sortedBy { it.first }
