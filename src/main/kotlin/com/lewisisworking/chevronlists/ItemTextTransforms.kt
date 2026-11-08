/**
 * ItemTextTransforms.kt
 * Pure logic for changing the case of an item's words and striking them through.
 * Markers, votes, comments and references are left alone. No IntelliJ Platform
 * imports - fully testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** A [[link]], #tag, @mention or web address: changing its case changes what it points at */
private val REFERENCE = Regex("""\[\[.*?]]|(?<!\S)[#@]\S+|(?<!\S)\w+://\S+""")
private val WORD_START = Regex("""(?<![\p{L}\p{N}_'’])\p{L}""")

/** Pure: the first letter of each word capitalised */
fun toTitleCase(text: String): String = WORD_START.replace(text) { it.value.uppercase() }

/** Pure: applies a case change to the words of item content, leaving everything else alone */
fun changeItemWords(content: String, change: (String) -> String): String {
    val parts = splitItem(content)
    val body = StringBuilder()
    var from = 0
    for (reference in REFERENCE.findAll(parts.body)) {
        body.append(change(parts.body.substring(from, reference.range.first))).append(reference.value)
        from = reference.range.last + 1
    }
    body.append(change(parts.body.substring(from)))
    return joinItem(parts.copy(body = body.toString()))
}

private fun isStruck(body: String) = body.length > 4 && body.startsWith("~~") && body.endsWith("~~")

/** Pure: toggles ~~strikethrough~~ on the words only, so the checkbox and priority keep working */
fun toggleStrikethrough(content: String): String {
    val parts = splitItem(content)
    return when {
        isStruck(parts.body) -> joinItem(parts.copy(body = parts.body.substring(2, parts.body.length - 2)))
        parts.body.isEmpty() -> content
        else -> joinItem(parts.copy(body = "~~${parts.body}~~"))
    }
}

/** Pure: removes ~~strikethrough~~ from the words, if they have it */
fun removeStrikethrough(content: String): String {
    val parts = splitItem(content)
    return if (isStruck(parts.body)) joinItem(parts.copy(body = parts.body.substring(2, parts.body.length - 2))) else content
}

/** Pure: [line] with [change] applied to its content, or null when it is not an item */
fun transformItemLine(line: String, listPrefix: String, change: (String) -> String): String? {
    parseBullet(line, listPrefix)?.let { return "${it.chevrons} ${it.prefix} ${change(it.content)}" }
    parseNumbered(line)?.let { return "${it.chevrons} ${it.num}. ${change(it.content)}" }
    return null
}
