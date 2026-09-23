/**
 * MoveItems.kt
 * Pure logic for moving a chevron item up or down within its section. No
 * IntelliJ Platform imports - fully testable with plain JUnit.
 *
 * An item moves as a block: the item itself plus anything nested under it, so a
 * parent never ends up below its own children. It swaps with the previous or
 * next sibling (an item at the same depth in the same section), and never
 * crosses a `> Header`.
 */
package com.lewisisworking.chevronlists

/** The document after a move, and the line the moved item now starts on */
data class MoveResult(val lines: List<String>, val newIndex: Int)

/**
 * Pure: the first and last line that differ between two versions of a document,
 * so only that span has to be rewritten. Null when they are identical, or when
 * the line counts differ - a move never changes the number of lines, and
 * rewriting a mismatched span would corrupt the file.
 */
fun changedSpan(before: List<String>, after: List<String>): IntRange? {
    if (before.size != after.size || before == after) return null
    var first = 0
    while (before[first] == after[first]) first++
    var last = before.size - 1
    while (before[last] == after[last]) last--
    return first..last
}

/** The chevron depth of an item line, or null if the line is not an item */
private fun depthOf(line: String, listPrefix: String): Int? =
    parseNumbered(line)?.chevrons?.length ?: parseBullet(line, listPrefix)?.chevrons?.length

/** The section holding [index]: the lines after the header above it, up to the next header */
private fun sectionRange(lines: List<String>, index: Int): IntRange {
    var start = 0
    for (i in index downTo 0) {
        if (isHeader(lines[i])) { start = i + 1; break }
    }
    var end = lines.size - 1
    for (i in index + 1 until lines.size) {
        if (isHeader(lines[i])) { end = i - 1; break }
    }
    return start..end
}

/** The last line of the block starting at [index]: the item plus anything nested under it */
private fun blockEnd(lines: List<String>, index: Int, depth: Int, section: IntRange, listPrefix: String): Int {
    var end = index
    for (i in index + 1..section.last) {
        val d = depthOf(lines[i], listPrefix) ?: break
        if (d <= depth) break
        end = i
    }
    return end
}

/**
 * Pure: moves the item at [index] one place up (or down when [up] is false) among
 * its siblings, carrying its nested children with it. Null when there is nothing
 * to do: the line is not an item, or it is already the first or last sibling of
 * its section.
 *
 * Numbered items keep the numbers they had; the auto-fix renumbers them, exactly
 * as it does after any other edit.
 */
fun computeMoveItem(lines: List<String>, index: Int, up: Boolean, listPrefix: String): MoveResult? {
    if (index !in lines.indices) return null
    val depth   = depthOf(lines[index], listPrefix) ?: return null
    val section = sectionRange(lines, index)
    if (index !in section) return null

    val end   = blockEnd(lines, index, depth, section, listPrefix)
    val block = lines.subList(index, end + 1).toList()

    if (up) {
        // The previous sibling: the nearest earlier line at the same depth
        var sibling = -1
        for (i in index - 1 downTo section.first) {
            val d = depthOf(lines[i], listPrefix) ?: continue
            if (d < depth) break          // left this item's parent, so there is no earlier sibling
            if (d == depth) { sibling = i; break }
        }
        if (sibling < 0) return null

        val rest = lines.toMutableList()
        rest.subList(index, end + 1).clear()
        rest.addAll(sibling, block)
        return MoveResult(rest, sibling)
    }

    // The next sibling, and the end of its own block: this item lands after it
    var sibling = -1
    for (i in end + 1..section.last) {
        val d = depthOf(lines[i], listPrefix) ?: continue
        if (d < depth) break
        if (d == depth) { sibling = i; break }
    }
    if (sibling < 0) return null
    val siblingEnd = blockEnd(lines, sibling, depth, section, listPrefix)

    val rest = lines.toMutableList()
    rest.subList(index, end + 1).clear()
    val insertAt = siblingEnd - block.size + 1
    rest.addAll(insertAt, block)
    return MoveResult(rest, insertAt)
}
