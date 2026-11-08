/**
 * SectionRename.kt
 * Pure logic for renaming the `> Header` section at the caret and the
 * `[[Name]]` links to it. No IntelliJ Platform imports - fully testable with
 * plain JUnit. Ported from the VS Code extension's Rename Section.
 */
package com.lewisisworking.chevronlists

/** The document after a rename, and how many other lines had a link rewritten */
data class SectionRename(val lines: List<String>, val linkLines: Int)

/** Pure: the caret section's header line and name, or null outside a section */
fun sectionNameAt(lines: List<String>, caretLine: Int): Pair<Int, String>? {
    if (caretLine !in lines.indices) return null
    val header = if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine) ?: return null
    return header to parseHeader(lines[header])!!.content.trim()
}

/** Pure: renames the section headed at [header] to [newName], rewriting every `[[Old name]]` link */
fun renameSection(lines: List<String>, header: Int, newName: String): SectionRename {
    val oldLink = "[[${parseHeader(lines[header])!!.content.trim()}]]"
    val name = newName.trim()
    var linkLines = 0
    val renamed = lines.mapIndexed { i, line ->
        when {
            i == header           -> "> $name"
            oldLink in line       -> { linkLines++; line.replace(oldLink, "[[$name]]") }
            else                  -> line
        }
    }
    return SectionRename(renamed, linkLines)
}
