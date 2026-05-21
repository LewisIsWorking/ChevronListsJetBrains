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