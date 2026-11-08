/**
 * ItemBulkEdits.kt
 * Pure logic for creation stamps, bulk tags, expiry dates and pasting a list.
 * No IntelliJ Platform imports - fully testable with plain JUnit. Ported from
 * the VS Code extension.
 */
package com.lewisisworking.chevronlists

private val CREATED_STAMP = Regex("""@created:\d{4}-\d{2}-\d{2}""")
private val EXPIRY_DATE   = Regex("""@expires:\d{4}-\d{2}-\d{2}""")
private val PASTED_DEPTH  = Regex("""^>{2,}\s+""")
private val PASTED_MARKER = Regex("""^(?:[-*+•]|\d+[.)])\s+""")
private val PASTED_BULLET = Regex("""^[-*+•]\s+""")
private val PASTED_NUMBER = Regex("""^(\d+)[.)]\s+(.*)$""")
private val PASTED_TITLE  = Regex("""^(?:>+|#+)\s*""")

private fun appendToWords(content: String, words: String): String {
    val parts = splitItem(content)
    return joinItem(parts.copy(body = "${parts.body} $words".trim()))
}

/** Pure: whether the item's words hold a @created: stamp. One in a comment does not count. */
fun hasCreatedStamp(content: String): Boolean = CREATED_STAMP.containsMatchIn(splitItem(content).body)

/** Pure: [content] with @created:[date] among its words, so a vote and comment stay last */
fun stampCreated(content: String, date: String): String = appendToWords(content, "@created:$date")

/** Pure: [content] with #[tag] among its words, unchanged when they already hold that tag in any case */
fun addItemTag(content: String, tag: String): String {
    val present = Regex("""(?<!\S)#${Regex.escape(tag)}(?![\w-])""", RegexOption.IGNORE_CASE)
    return if (present.containsMatchIn(splitItem(content).body)) content else appendToWords(content, "#$tag")
}

/** Pure: [content] with its expiry among the words set to [date] */
fun setItemExpiry(content: String, date: String): String {
    val parts = splitItem(content)
    if (!EXPIRY_DATE.containsMatchIn(parts.body)) return appendToWords(content, "@expires:$date")
    return joinItem(parts.copy(body = EXPIRY_DATE.replaceFirst(parts.body, "@expires:$date")))
}

private fun clipboardLines(clip: String): List<String> = clip.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

/** Pure: the words of each non-blank copied line. A marker already on one is replaced, not stacked. */
fun pastedWords(clip: String): List<String> =
    clipboardLines(clip).map { PASTED_MARKER.replaceFirst(PASTED_DEPTH.replaceFirst(it, ""), "") }

/** Pure: copied lines as bullet items */
fun pastedBullets(clip: String, listPrefix: String): List<String> = pastedWords(clip).map { ">> $listPrefix $it" }

/** Pure: copied lines as numbered items, continuing from the highest top-level number in the caret's section */
fun pastedNumbered(clip: String, lines: List<String>, caretLine: Int): List<String> {
    val header = headerAtOrAbove(lines, caretLine)
    val highest = if (header == null) 0 else (header + 1..sectionLastLine(lines, header))
        .mapNotNull { parseNumbered(lines[it]) }
        .filter { it.chevrons == ">>" }
        .maxOfOrNull { it.num } ?: 0
    return pastedWords(clip).mapIndexed { i, words -> ">> ${highest + i + 1}. $words" }
}

/** Pure: copied lines as a section, the first naming it and a numbered line staying numbered; null when there are none */
fun pastedSection(clip: String, listPrefix: String): List<String>? {
    val copied = clipboardLines(clip)
    if (copied.isEmpty()) return null
    val items = copied.drop(1).map { line ->
        val text = PASTED_DEPTH.replaceFirst(line, "")
        val numbered = PASTED_NUMBER.find(text)
        if (numbered != null) ">> ${numbered.groupValues[1]}. ${numbered.groupValues[2]}"
        else ">> $listPrefix ${PASTED_BULLET.replaceFirst(text, "")}"
    }
    return listOf("> ${PASTED_TITLE.replaceFirst(copied[0], "")}") + items
}

/**
 * Pure: [lines] with a new section [block]. On a header or outside any section
 * it goes at the caret line, followed by a blank line. Inside a section it goes
 * after that section's content, so items below the caret stay where they are.
 */
fun insertSectionBlock(lines: List<String>, caretLine: Int, block: List<String>): List<String> {
    val header = headerAtOrAbove(lines, caretLine)
    if (header == null || header == caretLine) {
        val gap = if (lines[caretLine].isBlank()) emptyList() else listOf("")
        return lines.take(caretLine) + block + gap + lines.drop(caretLine)
    }
    val end = sectionContentEnd(lines, header)
    return lines.take(end + 1) + "" + block + lines.drop(end + 1)
}
