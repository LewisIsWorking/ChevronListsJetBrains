/**
 * SortItems.kt
 * Pure logic for sorting the items of a section A to Z (or Z to A). No IntelliJ
 * Platform imports - fully testable with plain JUnit.
 *
 * Siblings are sorted, not lines: each item moves with everything nested under
 * it, and the nested items are sorted among themselves the same way. Lines
 * that are not items (blank lines, notes) stay where they are and split the
 * lists around them. Numbers stay with their positions, so a list that read
 * 1, 2, 3 still does.
 */
package com.lewisisworking.chevronlists

private data class Item(val head: String, val depth: Int, val content: String, val number: Int?)

// The depth comes from the parsed item, not chevronDepth: an item such as
// ">>\t- x" parses, but chevronDepth only accepts a space after the chevrons.
private fun itemOf(line: String, listPrefix: String): Item? {
    parseNumbered(line)?.let { return Item(line, it.chevrons.length, it.content, it.num) }
    parseBullet(line, listPrefix)?.let { return Item(line, it.chevrons.length, it.content, null) }
    return null
}

/** Sorts one region: groups of sibling blocks, each block's children sorted recursively */
private fun sortRegion(lines: List<String>, descending: Boolean, listPrefix: String): List<String> {
    val out = ArrayList<String>(lines.size)
    var i = 0
    while (i < lines.size) {
        val first = itemOf(lines[i], listPrefix)
        if (first == null) { out += lines[i]; i++; continue }
        val depth = first.depth

        // A run of siblings: items at this depth, each with the deeper items under it
        val blocks = mutableListOf<Pair<Item, List<String>>>()
        while (i < lines.size) {
            val item = itemOf(lines[i], listPrefix) ?: break
            if (item.depth != depth) break
            var end = i + 1
            while (end < lines.size && (itemOf(lines[end], listPrefix)?.depth ?: 0) > depth) end++
            blocks += item to sortRegion(lines.subList(i + 1, end), descending, listPrefix)
            i = end
        }

        val order = compareBy<Pair<Item, List<String>>> { it.first.content.lowercase() }
        val sorted = blocks.sortedWith(if (descending) order.reversed() else order)

        // Numbers belong to positions: hand the original numbers out again in order
        val numbers = blocks.mapNotNull { it.first.number }.sorted().iterator()
        for ((item, children) in sorted) {
            out += if (item.number != null) {
                val n = parseNumbered(item.head)!!
                "${n.chevrons} ${numbers.next()}. ${n.content}"
            } else item.head
            out += children
        }
    }
    return out
}

/**
 * Pure: the document with the items of the section holding [caretLine] sorted,
 * or null when nothing would change (not in a section with two or more
 * siblings, or already in order).
 */
fun computeSortSection(lines: List<String>, caretLine: Int, descending: Boolean, listPrefix: String): List<String>? {
    if (caretLine !in lines.indices) return null
    // The section: after the header at or above the caret, up to the next header
    var start = 0
    for (i in caretLine downTo 0) if (isHeader(lines[i])) { start = i + 1; break }
    var end = lines.size
    for (i in start until lines.size) if (isHeader(lines[i])) { end = i; break }

    val sorted = sortRegion(lines.subList(start, end), descending, listPrefix)
    val result = lines.subList(0, start) + sorted + lines.subList(end, lines.size)
    return if (result == lines) null else result
}
