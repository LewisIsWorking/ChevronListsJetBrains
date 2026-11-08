/**
 * ItemParts.kt
 * Pure logic that splits item content into the parts the parsers read from fixed
 * places, so a command can change the words without disturbing them:
 *
 *   [x] !!! * {red}  the words the user wrote  +3 // a note
 *   ^ lead markers   ^ body                    ^vote ^comment
 *
 * No IntelliJ Platform imports - fully testable with plain JUnit. Ported from the
 * VS Code extension's itemParts.ts.
 */
package com.lewisisworking.chevronlists

data class ItemParts(
    val check:    String,
    val priority: String,
    val marker:   String,
    val colour:   String,
    val body:     String,
    val vote:     Int?,
    val comment:  String?
)

private val LEAD_MARKERS = listOf(
    "check"    to Regex("""^\[(?:x| ?)]\s*""", RegexOption.IGNORE_CASE),
    "priority" to Regex("""^!{1,3}(?: +|$)"""),
    "marker"   to Regex("""^[*?](?: +|$)"""),
    "colour"   to Regex("""^\{(?:red|green|blue|yellow|orange|purple)}(?:\s+|$)"""),
)
private val ITEM_COMMENT = Regex("""(?:^|\s+)//\s*(.*)$""")
private val ITEM_VOTE    = Regex("""(?:^|\s+)\+(\d+)\s*$""")
private val EXTRA_SPACES = Regex("""\s{2,}""")

/** Pure: splits item content into its parts. Leading markers are accepted in any order. */
fun splitItem(content: String): ItemParts {
    val lead = mutableMapOf<String, String>()
    var rest = content.trim()
    var found = true
    while (found) {
        found = false
        for ((key, marker) in LEAD_MARKERS) {
            if (key in lead) continue
            val match = marker.find(rest) ?: continue
            lead[key] = match.value.trim()
            rest = rest.substring(match.value.length)
            found = true
        }
    }
    val comment = ITEM_COMMENT.find(rest)
    if (comment != null) rest = rest.substring(0, comment.range.first).trimEnd()
    val voteMatch = ITEM_VOTE.find(rest)
    val vote = voteMatch?.groupValues?.get(1)?.toIntOrNull()
    if (vote != null) rest = rest.substring(0, voteMatch.range.first)
    return ItemParts(
        check    = lead["check"] ?: "",
        priority = lead["priority"] ?: "",
        marker   = lead["marker"] ?: "",
        colour   = lead["colour"] ?: "",
        body     = rest.trim(),
        vote     = vote,
        comment  = comment?.groupValues?.get(1)
    )
}

/** Pure: joins parts back into content, markers in the order the parsers read them */
fun joinItem(parts: ItemParts): String {
    val check = when {
        parts.check.equals("[x]", ignoreCase = true) -> "[x]"
        parts.check.isEmpty() -> ""
        else -> "[ ]"
    }
    return listOf(
        check, parts.priority, parts.marker, parts.colour,
        parts.body.replace(EXTRA_SPACES, " ").trim(),
        parts.vote?.let { "+$it" } ?: "",
        parts.comment?.let { "// $it" } ?: "",
    ).filter { it.isNotEmpty() }.joinToString(" ")
}
