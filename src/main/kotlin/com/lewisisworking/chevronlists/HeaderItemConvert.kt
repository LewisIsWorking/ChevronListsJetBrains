/**
 * HeaderItemConvert.kt
 * Pure logic for turning an item into a `> Header` and a header back into an
 * item. No IntelliJ Platform imports - fully testable with plain JUnit. Ported
 * from the VS Code extension's Promote Item to Header / Demote Header to Item.
 */
package com.lewisisworking.chevronlists

/** Pure: the item on [line] as a `> Header`, or null when the line is not an item */
fun itemAsHeader(lines: List<String>, line: Int, listPrefix: String): String? {
    if (line !in lines.indices) return null
    val content = parseBullet(lines[line], listPrefix)?.content ?: parseNumbered(lines[line])?.content ?: return null
    return "> $content"
}

/**
 * Pure: the caret section's header line and its text as an item of the section
 * above, or null outside a section or in the first one. Its items then follow it
 * in the section above.
 */
fun headerAsItem(lines: List<String>, caretLine: Int, listPrefix: String): Pair<Int, String>? {
    if (caretLine !in lines.indices) return null
    val header = if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine) ?: return null
    findHeaderAbove(lines, header) ?: return null
    return header to ">> $listPrefix ${parseHeader(lines[header])!!.content}"
}
