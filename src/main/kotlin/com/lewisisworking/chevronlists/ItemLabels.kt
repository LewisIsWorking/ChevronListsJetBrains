/**
 * ItemLabels.kt
 * Pure logic for star ratings, set priorities, a section's URLs and finding a
 * section by name. No IntelliJ Platform imports - fully testable with plain
 * JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

private val RATING   = Regex("""★[1-5]\s*""")
private val URL      = Regex("""https?://[^\s)"'>]+""")
private val URL_TAIL = Regex("""[.,;:!?\]]+$""")

/** Pure: [content] rated [rating] stars among its words; 0 removes the rating */
fun setItemRating(content: String, rating: Int): String {
    val parts = splitItem(content)
    val words = RATING.replace(parts.body, "").trim()
    return joinItem(parts.copy(body = if (rating > 0) "★$rating $words".trim() else words))
}

/** Pure: [content] with a priority of [level] exclamation marks, replacing any it has */
fun setItemPriority(content: String, level: Int): String =
    joinItem(splitItem(content).copy(priority = "!".repeat(level)))

/** Pure: the distinct URLs in the items of the caret's section; null outside a section */
fun sectionUrls(lines: List<String>, caretLine: Int, listPrefix: String): List<String>? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    return (header + 1..sectionLastLine(lines, header))
        .mapNotNull { itemContent(lines[it], listPrefix) }
        .flatMap { content -> URL.findAll(content).map { URL_TAIL.replace(it.value, "") } }
        .distinct()
}

/** Pure: the line of the section called [name] in any case; null when there is none */
fun findSectionNamed(lines: List<String>, name: String): Int? =
    lines.indexOfFirst { parseHeader(it)?.content?.trim().equals(name.trim(), ignoreCase = true) }.takeIf { it >= 0 }
