/**
 * SectionCopy.kt
 * Pure logic for copying the caret section as standard Markdown or plain text.
 * No IntelliJ Platform imports - fully testable with plain JUnit. Ported from
 * the VS Code extension's Copy Section as Markdown / Plain Text.
 */
package com.lewisisworking.chevronlists

/** The items of the section headed at [header], up to the next header or markdown heading */
private fun sectionItems(lines: List<String>, header: Int, listPrefix: String): List<Pair<Int, String>> =
    (header + 1 until lines.size)
        .asSequence()
        .takeWhile { !isHeader(lines[it]) && parseSubheading(lines[it]) == null }
        .mapNotNull { i ->
            parseNumbered(lines[i])?.let { it.chevrons.length to "${it.num}. ${it.content}" }
                ?: parseBullet(lines[i], listPrefix)?.let { it.chevrons.length to "- ${it.content}" }
        }
        .toList()

private fun headerText(lines: List<String>, header: Int) = parseHeader(lines[header])?.content ?: lines[header]

/**
 * Pure: the section at [caretLine] as a `##` heading and a Markdown list, or null
 * when the caret is in no section. A nested item is indented to its parent's
 * text, so it still nests under a numbered item: CommonMark needs three spaces
 * under `1. `, and the two used for every level let it fall out of the list.
 */
fun sectionAsMarkdown(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = sectionHeaderAt(lines, caretLine).takeIf { it >= 0 } ?: return null
    // The text column of the latest item at each depth
    val textColumn = sortedMapOf<Int, Int>()
    val items = sectionItems(lines, header, listPrefix).map { (chevrons, item) ->
        val depth = chevrons - 2
        textColumn.keys.removeAll { it >= depth }
        // With no parent above it, two spaces a level as before
        val indent = textColumn.headMap(depth).values.lastOrNull() ?: (2 * depth)
        textColumn[depth] = indent + item.indexOf(' ') + 1
        " ".repeat(indent) + item
    }
    return (listOf("## ${headerText(lines, header)}", "") + items).joinToString("\n")
}

/** Pure: the section at [caretLine] as its header and item texts, one per line; null outside a section */
fun sectionAsPlainText(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = sectionHeaderAt(lines, caretLine).takeIf { it >= 0 } ?: return null
    val items = sectionItems(lines, header, listPrefix).map { (_, item) -> item.substringAfter(' ') }
    return (listOf(headerText(lines, header), "") + items).joinToString("\n")
}
