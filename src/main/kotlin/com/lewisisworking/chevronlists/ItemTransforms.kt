/**
 * ItemTransforms.kt
 * Pure logic for line-level transformations on chevron items - promote (decrease
 * chevron depth), demote (increase depth), and cycle list type (bullet <-> numbered).
 * No IntelliJ Platform imports - fully testable.
 */
package com.lewisisworking.chevronlists

/**
 * Pure: decreases the chevron depth by one. `>>> - foo` becomes `>> - foo`.
 * Returns null if the line is not a chevron item, or if it is already at depth 2
 * (the minimum for an item - depth 1 would make it a section header).
 */
fun computePromote(line: String, listPrefix: String): String? {
    val b = parseBullet(line, listPrefix)
    if (b != null) {
        if (b.chevrons.length <= 2) return null
        return ">".repeat(b.chevrons.length - 1) + " " + b.prefix + " " + b.content
    }
    val n = parseNumbered(line)
    if (n != null) {
        if (n.chevrons.length <= 2) return null
        return ">".repeat(n.chevrons.length - 1) + " " + n.num + ". " + n.content
    }
    return null
}

/**
 * Pure: increases the chevron depth by one. `>> - foo` becomes `>>> - foo`.
 * Returns null if the line is not a chevron item. No upper bound is enforced;
 * users may write items arbitrarily deep.
 */
fun computeDemote(line: String, listPrefix: String): String? {
    val b = parseBullet(line, listPrefix)
    if (b != null) {
        return ">".repeat(b.chevrons.length + 1) + " " + b.prefix + " " + b.content
    }
    val n = parseNumbered(line)
    if (n != null) {
        return ">".repeat(n.chevrons.length + 1) + " " + n.num + ". " + n.content
    }
    return null
}

/**
 * Pure: the lines of every child item nested under the item at [line] - each
 * chevron item below it whose depth is strictly greater. The first line that
 * is not a deeper item (same depth, a header, blank or plain text) ends the
 * block. Mirrors the VS Code extension, where Tab on one item carries the
 * items nested under it along.
 */
fun childItemLines(lines: List<String>, line: Int, listPrefix: String): List<Int> {
    if (line !in lines.indices) return emptyList()
    val depth = parseBullet(lines[line], listPrefix)?.chevrons?.length
        ?: parseNumbered(lines[line])?.chevrons?.length
        ?: return emptyList()
    val children = mutableListOf<Int>()
    for (i in line + 1..lines.lastIndex) {
        val childDepth = parseBullet(lines[i], listPrefix)?.chevrons?.length
            ?: parseNumbered(lines[i])?.chevrons?.length
            ?: break
        if (childDepth <= depth) break
        children.add(i)
    }
    return children
}

/**
 * Pure: cycles between bullet and numbered list types on the same line.
 *   `>> - foo`  becomes `>> 1. foo`
 *   `>> 1. foo` becomes `>> - foo`
 * Returns null if the line is not a chevron item. Numbered items always cycle
 * back to `1` since auto-fix-numbering will renumber the surrounding sequence.
 */
fun computeCycleListType(line: String, listPrefix: String): String? {
    val b = parseBullet(line, listPrefix)
    if (b != null) {
        return "${b.chevrons} 1. ${b.content}"
    }
    val n = parseNumbered(line)
    if (n != null) {
        return "${n.chevrons} $listPrefix ${n.content}"
    }
    return null
}