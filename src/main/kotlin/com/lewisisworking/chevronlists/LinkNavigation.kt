/**
 * LinkNavigation.kt
 * Pure logic for following [[links]], searching items, listing dependencies and
 * writing an item in readable form. No IntelliJ Platform imports - fully
 * testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** Where a `[[file:name.md#Section]]` link points; [section] is null without a `#` */
data class FileLinkTarget(val filename: String, val section: String?)

private val ANY_LINK       = Regex("""\[\[([^]]+)]]""")
private val FILE_LINK      = Regex("""\[\[file:([^]#]+)(?:#([^]]+))?]]""")
private val DEPENDS        = Regex("""^>>depends:(.+)$""")
private val STRUCK         = Regex("""~~([^~]+)~~""")
private val COLOUR_LABEL   = Regex("""\{(?:red|green|blue|yellow|orange|purple)}\s*""", RegexOption.IGNORE_CASE)
private val DUE_MARK       = Regex("""@(\d{4}-\d{2}-\d{2})""")
private val ESTIMATE_MARK  = Regex("""~(\d+h(?:\d+m)?|\d+m)(?=\s|$)""")
private val RATING_MARK    = Regex("""★(\d)""")
private val PRIORITY_SIGNS = mapOf("!!!" to "🔴", "!!" to "🟠", "!" to "🟡")
private val MARKER_SIGNS   = mapOf("*" to "⭐", "?" to "❓")

private fun covers(match: MatchResult, column: Int) = column in match.range.first..match.range.last + 1

/** Pure: the section named by the `[[link]]` at [column], never a `[[file:...]]` link */
fun sectionLinkAt(line: String, column: Int): String? =
    ANY_LINK.findAll(line).firstOrNull { covers(it, column) }?.groupValues?.get(1)?.trim()?.takeUnless { it.startsWith("file:") }

/** Pure: the target of the `[[file:...]]` link at [column] */
fun fileLinkAt(line: String, column: Int): FileLinkTarget? =
    FILE_LINK.findAll(line).firstOrNull { covers(it, column) }?.let { match ->
        FileLinkTarget(match.groupValues[1].trim(), match.groupValues[2].trim().ifEmpty { null })
    }

/** Pure: the text of a link to [section] of the file called [fileName] */
fun fileSectionLink(fileName: String, section: String): String = "[[file:$fileName#$section]]"

/** Pure: every item containing [term], ignoring case, with the section it is in */
fun itemsMatching(lines: List<String>, listPrefix: String, term: String): List<ReportEntry> {
    var section = ""
    return lines.indices.mapNotNull { i ->
        if (isHeader(lines[i])) section = plainSectionName(lines, i)
        itemContent(lines[i], listPrefix)?.takeIf { it.contains(term, ignoreCase = true) }
            ?.let { ReportEntry(i, if (section.isEmpty()) it else "$it  ($section)") }
    }
}

/** Pure: every `>>depends:Name` line that is inside a section */
fun sectionDependencies(lines: List<String>): List<ReportEntry> {
    var section = ""
    return lines.indices.mapNotNull { i ->
        if (isHeader(lines[i])) section = plainSectionName(lines, i)
        DEPENDS.find(lines[i])?.takeIf { section.isNotEmpty() }
            ?.let { ReportEntry(i, "$section depends on ${it.groupValues[1].trim()}") }
    }
}

/** Pure: the item on [line] with its markers written out, indented by depth; null if it is not an item */
fun richItemText(line: String, listPrefix: String): String? {
    val bullet   = parseBullet(line, listPrefix)
    val numbered = parseNumbered(line)
    val content  = bullet?.content ?: numbered?.content ?: return null
    val depth    = (bullet?.chevrons ?: numbered!!.chevrons).length - 2
    val parts    = splitItem(content)
    val lead = buildString {
        if (parts.check.isNotEmpty()) append(if (parts.check.contains('x', ignoreCase = true)) "✓ " else "☐ ")
        PRIORITY_SIGNS[parts.priority]?.let { append(it).append(' ') }
        MARKER_SIGNS[parts.marker]?.let { append(it).append(' ') }
    }
    var body = STRUCK.replace(parts.body, "(\$1)")
    body = COLOUR_LABEL.replace(body, "")
    body = DUE_MARK.replace(body, "(due: \$1)")
    body = ESTIMATE_MARK.replace(body, "(~\$1)")
    body = RATING_MARK.replaceFirst(body, "(★\$1)")
    val vote = parts.vote?.let { " (+$it)" } ?: ""
    val note = parts.comment?.let { " [note: $it]" } ?: ""
    return "  ".repeat(depth) + (lead + body + vote + note).trim()
}
