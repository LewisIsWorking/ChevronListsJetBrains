/**
 * ItemEdits.kt
 * Pure logic for merging and splitting items, sorting whole sections and toggling
 * text styles. No IntelliJ Platform imports - fully testable with plain JUnit.
 * Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

private val WORD_CHARS = Regex("""\w+""")
private const val COMBINING_LOW_LINE = "̲"

private fun withContent(line: String, listPrefix: String, content: String): String =
    transformItemLine(line, listPrefix) { content } ?: line

/** [lines] with every later item of the numbered list at [index] renumbered by [by] */
private fun shiftFollowing(lines: List<String>, index: Int, depth: Int, by: Int): List<String> {
    val out = lines.toMutableList()
    for (i in index + 1 until lines.size) {
        if (isHeader(lines[i])) break
        val d = chevronDepth(lines[i]) ?: continue
        if (d < depth) break
        if (d > depth) continue
        val n = parseNumbered(lines[i]) ?: continue
        out[i] = "${n.chevrons} ${n.num + by}. ${n.content}"
    }
    return out
}

/**
 * Pure: the item on [index] joined with the item below it as "first - second".
 * The first item's markers stay, a vote and comment on the second are kept, and
 * the list closes up behind it. Null unless both lines are items.
 */
fun mergeItemWithNext(lines: List<String>, index: Int, listPrefix: String): List<String>? {
    val next = index + 1
    if (index !in lines.indices || next !in lines.indices) return null
    val first = splitItem(itemContent(lines[index], listPrefix) ?: return null)
    val second = splitItem(itemContent(lines[next], listPrefix) ?: return null)
    val merged = joinItem(first.copy(
        body = "${first.body} - ${second.body}",
        vote = first.vote ?: second.vote,
        comment = listOfNotNull(first.comment, second.comment).joinToString("; ").ifEmpty { null }
    ))
    val depth = parseNumbered(lines[next])?.chevrons?.length
    val closed = if (depth == null) lines else shiftFollowing(lines, next, depth, -1)
    return closed.toMutableList().apply {
        this[index] = withContent(lines[index], listPrefix, merged)
        removeAt(next)
    }
}

/** A split: the new document, and the column of the second item's words on the line below */
data class ItemSplit(val lines: List<String>, val column: Int)

/** Pure: the item on [index] split at [column]; null unless the caret is inside its words */
fun splitItemAt(lines: List<String>, index: Int, column: Int, listPrefix: String): ItemSplit? {
    if (index !in lines.indices) return null
    val line = lines[index]
    val numbered = parseNumbered(line)
    val content = itemContent(line, listPrefix) ?: return null
    val start = line.length - content.length
    val at = column - start
    if (at !in 1 until content.length) return null
    val before = content.substring(0, at).trimEnd()
    val after = content.substring(at).trimStart()
    if (before.isEmpty() || after.isEmpty()) return null

    val head = if (numbered == null) line.substring(0, start) else "${numbered.chevrons} ${numbered.num + 1}. "
    val shifted = if (numbered == null) lines else shiftFollowing(lines, index, numbered.chevrons.length, 1)
    val out = shifted.toMutableList()
    out[index] = line.substring(0, start) + before
    out.add(index + 1, head + after)
    return ItemSplit(out, head.length)
}

/**
 * Pure: the sections sorted by name, anything before the first staying put.
 * Blank lines separate sections rather than belonging to one, so the gap and the
 * end of the document keep their shape. Null with fewer than two sections.
 */
fun sortAllSections(lines: List<String>, ascending: Boolean): List<String>? {
    val headers = lines.indices.filter { isHeader(lines[it]) }
    if (headers.size < 2) return null
    val blocks = headers.mapIndexed { n, start -> lines.subList(start, headers.getOrElse(n + 1) { lines.size }) }

    fun trailingBlanks(block: List<String>) = block.reversed().takeWhile { it.isBlank() }.size
    val gap = trailingBlanks(blocks.first())
    val end = trailingBlanks(blocks.last())

    val byName = compareBy<List<String>, String>(String.CASE_INSENSITIVE_ORDER) { parseHeader(it[0])!!.content }
    val sorted = blocks.sortedWith(if (ascending) byName else byName.reversed())
    val body = sorted.flatMapIndexed { n, block ->
        block.dropLast(trailingBlanks(block)) + List(if (n < sorted.lastIndex) gap else end) { "" }
    }
    return lines.subList(0, headers.first()) + body
}

/** Pure: [text] wrapped in [mark], or unwrapped when it already is */
fun toggleWrap(text: String, mark: String): String =
    if (text.startsWith(mark) && text.endsWith(mark) && text.length > 2 * mark.length)
        text.substring(mark.length, text.length - mark.length)
    else "$mark$text$mark"

/** Pure: [text] with a combining underline on every character, or without when it has any */
fun toggleUnderline(text: String): String =
    if (COMBINING_LOW_LINE in text) text.replace(COMBINING_LOW_LINE, "")
    else text.codePoints().toArray().joinToString("") { String(Character.toChars(it)) + COMBINING_LOW_LINE }

/** Pure: the range of the word touching [column] in [line], or null */
fun wordRangeAt(line: String, column: Int): IntRange? =
    WORD_CHARS.findAll(line).firstOrNull { column in it.range.first..it.range.last + 1 }?.range
