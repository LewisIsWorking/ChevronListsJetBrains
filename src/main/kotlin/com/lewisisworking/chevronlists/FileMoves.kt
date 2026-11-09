/**
 * FileMoves.kt
 * Pure logic for moving an item or copying a section into another file. No
 * IntelliJ Platform imports - fully testable with plain JUnit. Ported from the
 * VS Code extension.
 */
package com.lewisisworking.chevronlists

private fun blockDepth(line: String, listPrefix: String): Int =
    (parseNumbered(line)?.chevrons ?: parseBullet(line, listPrefix)?.chevrons)?.length ?: 0

/** Pure: the lines of the item at [line] and everything nested under it; null when it is not an item */
fun itemBlockRange(lines: List<String>, line: Int, listPrefix: String): IntRange? {
    val depth = blockDepth(lines[line], listPrefix).takeIf { it > 0 } ?: return null
    var end = line
    while (end + 1 < lines.size && blockDepth(lines[end + 1], listPrefix) > depth) end++
    return line..end
}

/** Pure: [dest] with [block] added after the last item of the section at [header], before its blank lines */
fun withBlockInSection(dest: List<String>, header: Int, block: List<String>): List<String> {
    val end = (sectionLastLine(dest, header) downTo header).first { it == header || dest[it].isNotBlank() }
    return dest.take(end + 1) + block + dest.drop(end + 1)
}

/** Pure: the section at [header] without the blank lines that end it */
fun sectionBlock(lines: List<String>, header: Int): List<String> =
    lines.subList(header, sectionLastLine(lines, header) + 1).dropLastWhile { it.isBlank() }

/** Pure: [dest] with [section] added at the end, a blank line before it and a final newline after it */
fun withSectionAtEnd(dest: List<String>, section: List<String>): List<String> {
    val kept = dest.dropLastWhile { it.isBlank() }
    return (if (kept.isEmpty()) emptyList() else kept + "") + section + ""
}
