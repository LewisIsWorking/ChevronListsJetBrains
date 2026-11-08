/**
 * ItemClones.kt
 * Pure logic for the item clone variants. No IntelliJ Platform imports - fully
 * testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

/**
 * Pure: a copy of just the item on [index], its content passed through [change],
 * below the item and its children. The children stay with the original. Null when
 * the line is not an item.
 */
fun itemCopyBelow(lines: List<String>, index: Int, listPrefix: String, change: (String) -> String): DuplicateEdit? {
    val edit = computeDuplicateItem(lines, index, listPrefix) ?: return null
    return edit.copy(insert = listOf(transformItemLine(edit.insert[0], listPrefix, change)!!))
}

/** Pure: [content] with a ticked checkbox, replacing any it already has */
fun markCloneDone(content: String): String = joinItem(splitItem(content).copy(check = "[x]"))

private val LEADING_CHECKBOX = Regex("""^\[(?:x| ?)]\s*""", RegexOption.IGNORE_CASE)

/** Removed in this order: each marker only where its parser recognises it, so ordinary prose survives */
private val METADATA = listOf(
    LEADING_CHECKBOX,
    Regex("""^!{1,3} +"""),
    Regex("""^\* +"""),
    Regex("""^\? +"""),
    Regex("""(?:^|\s+)//.*$"""),
    Regex("""(?:^|\s+)\+\d+\s*$"""),
    Regex("""(?<!\S)#\w[\w-]*"""),
    Regex("""@\d{4}-\d{2}-\d{2}"""),
    Regex("""@created:\d{4}-\d{2}-\d{2}"""),
    Regex("""@(?:daily|weekly|monthly)"""),
    Regex("""~\d+h(?:\d+m)?"""),
    Regex("""~\d+m"""),
    Regex("""\{(?:red|green|blue|yellow|orange|purple)}\s*"""),
)
private val STRUCK       = Regex("""~~(.+?)~~""")
private val SECTION_LINK = Regex("""\[\[[\w\s]+]]""")
private val FILE_LINK    = Regex("""\[\[file:[^]]+]]""")
private val WHITESPACE   = Regex("""\s{2,}""")

/** Pure: item content with every known marker removed, leaving the plain words */
fun stripItemMetadata(content: String): String {
    var text = content.trim()
    for (marker in METADATA) text = marker.replace(text, "")
    text = STRUCK.replace(text) { it.groupValues[1] }
    text = FILE_LINK.replace(SECTION_LINK.replace(text, ""), "")
    return WHITESPACE.replace(text, " ").trim()
}

private val ANY_LINK = Regex("""\[\[[^]]*]]""")
private val WORD     = Regex("""\S+""")
private val DIGITS   = Regex("""\d+""")

/** Pure: [content] with its first plain number one higher, or null when it has none. Tags, dates, votes and links do not count. */
fun incrementFirstNumber(content: String): String? {
    val masked = ANY_LINK.replace(content) { " ".repeat(it.value.length) }
    for (word in WORD.findAll(masked)) {
        if (word.value[0] in "@~+★#") continue
        val digits = DIGITS.find(word.value) ?: continue
        val start = word.range.first + digits.range.first
        val next = (digits.value.toBigInteger() + java.math.BigInteger.ONE).toString().padStart(digits.value.length, '0')
        return content.replaceRange(start, start + digits.value.length, next)
    }
    return null
}

/** Pure: a copy of the item on [index] at the end of its section, or null outside a section. A numbered copy continues the list. */
fun cloneItemToSectionEnd(lines: List<String>, index: Int, listPrefix: String): DuplicateEdit? {
    if (index !in lines.indices) return null
    val numbered = parseNumbered(lines[index])
    if (numbered == null && parseBullet(lines[index], listPrefix) == null) return null
    val header = findHeaderAbove(lines, index) ?: return null
    val end = sectionContentEnd(lines, header)
    val copy = if (numbered == null) lines[index] else {
        val last = (header + 1..end).mapNotNull { parseNumbered(lines[it]) }.last { it.chevrons == numbered.chevrons }
        "${numbered.chevrons} ${last.num + 1}. ${numbered.content}"
    }
    return DuplicateEdit(end, listOf(copy), emptyMap())
}
