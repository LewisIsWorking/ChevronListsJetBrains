/**
 * GroupsSnippets.kt
 * Pure logic for section groups and item snippets. No IntelliJ Platform imports -
 * fully testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** The sections below the `>> -- Name` divider on [line], up to the next divider */
data class SectionGroup(val name: String, val sections: List<String>, val line: Int)

/** A snippet's text with its first placeholder's range */
data class ExpandedSnippet(val text: String, val selectionStart: Int, val selectionEnd: Int)

private val GROUP_DIVIDER = Regex("""^>> -- (.+)$""")
private val PLACEHOLDER   = Regex("""\$\{(\d+)(?::([^}]*)|\|([^,|}]*)[^}]*)}""")

/** Pure: every group of the file that has a section in it */
fun sectionGroups(lines: List<String>): List<SectionGroup> {
    val groups = mutableListOf<SectionGroup>()
    for ((i, line) in lines.withIndex()) {
        val divider = GROUP_DIVIDER.find(line)
        if (divider != null) groups += SectionGroup(divider.groupValues[1].trim(), emptyList(), i)
        else if (isHeader(line) && groups.isNotEmpty()) {
            groups[groups.lastIndex] = groups.last().let { it.copy(sections = it.sections + plainSectionName(lines, i)) }
        }
    }
    return groups.filter { it.sections.isNotEmpty() }
}

/** Pure: one entry per group, naming its sections */
fun groupEntries(lines: List<String>): List<ReportEntry> = sectionGroups(lines).map {
    ReportEntry(it.line, "${it.name}  (${plural(it.sections.size, "section")}: ${it.sections.joinToString(", ")})")
}

/** Pure: the divider line that starts a group called [name] */
fun groupDivider(name: String): String = ">> -- ${name.trim()}"

/** The snippets Insert Item Snippet offers, by name, with [today] as every default date */
fun itemSnippets(today: String): List<Pair<String, String>> = listOf(
    "Task"           to "[ ] \${1:task} @\${2:$today} #\${3:tag}",
    "Urgent Task"    to "[ ] !!! \${1:task} @\${2:$today}",
    "Starred Note"   to "* \${1:note}",
    "Flagged Item"   to "? \${1:unclear thing}",
    "Timed Task"     to "\${1:task} ~\${2:1h}",
    "Assigned Task"  to "\${1:task} @\${2:Person}",
    "Coloured Item"  to "{\${1|red,green,blue,yellow|}} \${2:item}",
    "Voted Item"     to "\${1:item} +\${2:1}",
    "Commented Item" to "\${1:item} // \${2:note}",
    "Stamped Item"   to "\${1:item} @created:\${2:$today}",
)

/** Pure: [template] with each placeholder replaced by its default, and where the first one ended up */
fun expandItemSnippet(template: String): ExpandedSnippet {
    val text  = StringBuilder()
    var start = 0
    var end   = 0
    var from  = 0
    for (match in PLACEHOLDER.findAll(template)) {
        text.append(template, from, match.range.first)
        val default = match.groupValues[2].ifEmpty { match.groupValues[3] }
        if (match.groupValues[1] == "1") {
            start = text.length
            end   = start + default.length
        }
        text.append(default)
        from = match.range.last + 1
    }
    text.append(template, from, template.length)
    return ExpandedSnippet(text.toString(), start, end)
}
