/**
 * ItemFilters.kt
 * Pure lists of the items matching a marker, rating, colour, vote or expiry date.
 * No IntelliJ imports. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

private const val STAR_EMOJI = "⭐"
private const val FLAG_EMOJI = "🚩"

private val SPACES  = Regex("""\s+""")
private val EXPIRY  = Regex("""@expires:(\d{4}-\d{2}-\d{2})""")
private val RATING  = Regex("""★([1-5])\s*""")
private val COLOUR  = Regex("""\{(red|green|blue|yellow|orange|purple)}""")

internal class FoundItem(val line: Int, val content: String, val parts: ItemParts, val section: String?) {
    /** ` (Section)` for the end of a label, or nothing outside a section */
    val suffix get() = section?.let { "  ($it)" } ?: ""
}

internal fun foundItems(lines: List<String>, listPrefix: String): List<FoundItem> =
    lines.indices.mapNotNull { i ->
        itemContent(lines[i], listPrefix)?.let { content ->
            FoundItem(i, content, splitItem(content), headerAtOrAbove(lines, i)?.let { headerName(lines, it) })
        }
    }

private fun hasToken(body: String, token: String) = token in body.split(SPACES)

private fun withoutTokens(body: String, token: String) = body.split(SPACES).filter { it != token }.joinToString(" ")

internal fun tidied(text: String) = SPACES.replace(text, " ").trim()

/** Pure: whether an item is starred with `*` or the star emoji */
internal fun isStarred(parts: ItemParts) = parts.marker == "*" || hasToken(parts.body, STAR_EMOJI)

/** Pure: whether an item is flagged with `?` or the flag emoji */
internal fun isFlagged(parts: ItemParts) = parts.marker == "?" || hasToken(parts.body, FLAG_EMOJI)

/** Pure: the items with priority [level] (1 to 3), or with any priority when it is 0 */
fun itemsWithPriority(lines: List<String>, listPrefix: String, level: Int): List<ReportEntry> =
    foundItems(lines, listPrefix)
        .filter { it.parts.priority.isNotEmpty() && (level == 0 || it.parts.priority.length == level) }
        .map { ReportEntry(it.line, "${it.parts.priority} ${it.parts.body}${it.suffix}") }

/** Pure: the starred items */
fun starredItems(lines: List<String>, listPrefix: String): List<ReportEntry> =
    foundItems(lines, listPrefix).filter { isStarred(it.parts) }
        .map { ReportEntry(it.line, "$STAR_EMOJI ${withoutTokens(it.parts.body, STAR_EMOJI)}${it.suffix}") }

/** Pure: the flagged items */
fun flaggedItems(lines: List<String>, listPrefix: String): List<ReportEntry> =
    foundItems(lines, listPrefix).filter { isFlagged(it.parts) }
        .map { ReportEntry(it.line, "$FLAG_EMOJI ${withoutTokens(it.parts.body, FLAG_EMOJI)}${it.suffix}") }

/** Pure: the items whose @expires: date is before [today], an ISO date */
fun expiredItems(lines: List<String>, listPrefix: String, today: String): List<ReportEntry> =
    foundItems(lines, listPrefix).mapNotNull { item ->
        val date = EXPIRY.find(item.parts.body)?.groupValues?.get(1)?.takeIf { it < today } ?: return@mapNotNull null
        ReportEntry(item.line, "${tidied(EXPIRY.replace(item.parts.body, ""))}${item.suffix} - expired $date")
    }

/** Pure: the items rated at least [minimum] stars, highest first */
fun ratedItems(lines: List<String>, listPrefix: String, minimum: Int): List<ReportEntry> =
    foundItems(lines, listPrefix)
        .mapNotNull { item -> RATING.find(item.parts.body)?.groupValues?.get(1)?.toInt()?.let { it to item } }
        .filter { (rating, _) -> rating >= minimum }
        .sortedByDescending { (rating, _) -> rating }
        .map { (rating, item) -> ReportEntry(item.line, "${"★".repeat(rating)} ${tidied(RATING.replace(item.parts.body, ""))}${item.suffix}") }

private fun colourOf(parts: ItemParts) = COLOUR.find("${parts.colour} ${parts.body}")?.groupValues?.get(1)

/** Pure: each colour label in use with how many items carry it, most used first */
fun colourCounts(lines: List<String>, listPrefix: String): List<Pair<String, Int>> =
    foundItems(lines, listPrefix).mapNotNull { colourOf(it.parts) }
        .groupingBy { it }.eachCount().toList()
        .sortedByDescending { it.second }

/** Pure: the items labelled [colour] */
fun itemsWithColour(lines: List<String>, listPrefix: String, colour: String): List<ReportEntry> =
    foundItems(lines, listPrefix).filter { colourOf(it.parts) == colour }
        .map { ReportEntry(it.line, "{$colour} ${tidied(COLOUR.replace(it.parts.body, ""))}${it.suffix}") }

/** Pure: the items of the file with votes, most votes first */
fun voteLeaderboard(lines: List<String>, listPrefix: String): List<ReportEntry> =
    foundItems(lines, listPrefix).filter { (it.parts.vote ?: 0) > 0 }
        .sortedByDescending { it.parts.vote }
        .map { ReportEntry(it.line, "+${it.parts.vote} ${it.parts.body}${it.suffix}") }

/** Pure: the items of the caret's section with votes, ranked; null outside a section */
fun sectionVoteLeaderboard(lines: List<String>, caretLine: Int, listPrefix: String): List<ReportEntry>? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val last   = sectionLastLine(lines, header)
    return foundItems(lines, listPrefix)
        .filter { it.line in header + 1..last && (it.parts.vote ?: 0) > 0 }
        .sortedByDescending { it.parts.vote }
        .mapIndexed { rank, item -> ReportEntry(item.line, "#${rank + 1}  +${item.parts.vote} ${item.parts.body}") }
}

/** Pure: every item whose plain words appear more than once in the file */
fun duplicateItems(lines: List<String>, listPrefix: String): List<ReportEntry> =
    foundItems(lines, listPrefix)
        .groupBy { stripItemMetadata(it.content).lowercase() }
        .filter { (plain, found) -> plain.isNotEmpty() && found.size > 1 }
        .flatMap { (plain, found) ->
            found.mapIndexed { n, item ->
                val where = listOfNotNull(item.section, "line ${item.line + 1}").joinToString(", ")
                ReportEntry(item.line, "${if (n == 0) plain else "  duplicate"}  ($where)")
            }
        }
