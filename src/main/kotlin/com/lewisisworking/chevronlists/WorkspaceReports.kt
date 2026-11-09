/**
 * WorkspaceReports.kt
 * Pure logic for the commands that look across every markdown file of a project:
 * Today View, Find Dead Links, the workspace searches and filters, and the
 * workspace tag report and statistics. No IntelliJ Platform imports. Mirrors
 * todayViewCommands.ts, deadLinksCommands.ts, workspaceSearch.ts and friends.
 */
package com.lewisisworking.chevronlists

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** A markdown file read from the project: [path] is relative to the project where it can be */
data class ScannedFile(val path: String, val lines: List<String>) {
    val name: String get() = path.substringAfterLast('/')
}

/** A line to jump to in one of the project's files */
data class WorkspaceEntry(val path: String, val line: Int, val label: String)

private val DEAD_SECTION_LINK = Regex("""\[\[([^]#:]+)]]""")
private val DEAD_FILE_LINK    = Regex("""\[\[file:([^]#]+?)(?:#[^]]+)?]]""")

private fun whereIs(file: ScannedFile, section: String?) = if (section.isNullOrEmpty()) file.name else "${file.name} › $section"

/** Pure: open items due by [today] in every file, the latest first */
fun todayEntries(files: List<ScannedFile>, listPrefix: String, today: LocalDate): List<WorkspaceEntry> =
    files.flatMap { file ->
        foundItems(file.lines, listPrefix).mapNotNull { item ->
            if (item.parts.check.equals("[x]", ignoreCase = true)) return@mapNotNull null
            val due = ITEM_DUE_DATE.find(item.parts.body)?.groupValues?.get(1) ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(due) }.getOrNull() ?: return@mapNotNull null
            val days = ChronoUnit.DAYS.between(date, today)
            if (days < 0) return@mapNotNull null
            val text  = tidied(ITEM_DUE_DATE.replaceFirst(item.parts.body, ""))
            val state = if (days == 0L) "Today" else "${days}d overdue"
            days to WorkspaceEntry(file.path, item.line, "$state  $text  (${whereIs(file, item.section)})")
        }
    }.sortedByDescending { it.first }.map { it.second }

/** Pure: every `[[section]]` link with no such section in its file and `[[file:name]]` link with no such file */
fun deadLinks(files: List<ScannedFile>): List<WorkspaceEntry> {
    val names = files.map { it.name }.toSet()
    return files.flatMap { file ->
        val sections = file.lines.filter { isHeader(it) }.map { parseHeader(it)!!.content.trim().lowercase() }.toSet()
        file.lines.withIndex().flatMap { (i, line) ->
            val where = "${file.name}:${i + 1}"
            val deadSections = DEAD_SECTION_LINK.findAll(line).map { it.groupValues[1] }
                .filter { it.trim().lowercase() !in sections }
                .map { WorkspaceEntry(file.path, i, "[[$it]] (section not found)  $where") }
            val deadFiles = DEAD_FILE_LINK.findAll(line).map { it.groupValues[1] }
                .filter { it.trim() !in names }
                .map { WorkspaceEntry(file.path, i, "[[file:$it]] (file not found)  $where") }
            (deadSections + deadFiles).toList()
        }
    }
}

/** Pure: every item of every file */
fun workspaceItems(files: List<ScannedFile>, listPrefix: String): List<WorkspaceEntry> =
    files.flatMap { file ->
        foundItems(file.lines, listPrefix).map { WorkspaceEntry(file.path, it.line, "${it.content}  (${whereIs(file, it.section)})") }
    }

/** Pure: every section of every file */
fun workspaceSections(files: List<ScannedFile>): List<WorkspaceEntry> =
    files.flatMap { file ->
        file.lines.indices.filter { isHeader(file.lines[it]) }
            .map { WorkspaceEntry(file.path, it, "${plainSectionName(file.lines, it)}  (${file.name})") }
    }

/** Pure: the items carrying each tag across the files, tags in order */
fun workspaceTags(files: List<ScannedFile>, listPrefix: String): Map<String, List<WorkspaceEntry>> =
    files.flatMap { file ->
        tagHits(file.lines, listPrefix).map { it.tag to WorkspaceEntry(file.path, it.line, "${it.itemText}  (${whereIs(file, it.section)})") }
    }.groupBy({ it.first }, { it.second }).toSortedMap()

/** Pure: the items mentioning each person across the files, names in order */
fun workspaceMentions(files: List<ScannedFile>, listPrefix: String): Map<String, List<WorkspaceEntry>> =
    files.flatMap { file ->
        foundItems(file.lines, listPrefix).flatMap { item ->
            mentionsIn(item.parts).map { it to WorkspaceEntry(file.path, item.line, "${item.content}  (${whereIs(file, item.section)})") }
        }
    }.groupBy({ it.first }, { it.second }).toSortedMap()

/** Pure: the items carrying each colour label across the files, only colours in use */
fun workspaceColours(files: List<ScannedFile>, listPrefix: String): Map<String, List<WorkspaceEntry>> =
    ColourLabel.values().map { it.displayName }.associateWith { colour ->
        files.flatMap { file ->
            itemsWithColour(file.lines, listPrefix, colour).map { WorkspaceEntry(file.path, it.line, "${it.label}  ${file.name}") }
        }
    }.filterValues { it.isNotEmpty() }

/** Pure: a markdown report of each tag's use per file, most used first; null with no tags */
fun workspaceTagReport(files: List<ScannedFile>, listPrefix: String): String? {
    val byTag = files.flatMap { file -> tagHits(file.lines, listPrefix).map { it.tag to file.path } }
        .groupBy({ it.first }, { it.second })
    if (byTag.isEmpty()) return null
    val parts = byTag.entries.sortedByDescending { it.value.size }.map { (tag, paths) ->
        val perFile = paths.groupingBy { it }.eachCount().entries.joinToString("\n") { (path, n) -> "  - $path: ${plural(n, "item")}" }
        "## #$tag (${paths.size} total)\n\n$perFile"
    }
    return "# Tag Report - Workspace\n\n${plural(byTag.size, "unique tag")} across ${plural(files.size, "file")}\n\n${parts.joinToString("\n\n")}"
}

/** Pure: a markdown table of totals across the files; null with no files */
fun workspaceStatistics(files: List<ScannedFile>, listPrefix: String): String? {
    if (files.isEmpty()) return null
    val items  = files.flatMap { foundItems(it.lines, listPrefix) }
    val boxed  = items.filter { it.parts.check.isNotEmpty() }
    val done   = boxed.count { it.parts.check.equals("[x]", ignoreCase = true) }
    val pct    = if (boxed.isEmpty()) "n/a" else "${(done * 100.0 / boxed.size).roundToInt()}%"
    return listOf(
        "# Workspace Statistics", "",
        "| Metric | Value |", "|--------|-------|",
        "| Files | ${files.size} |",
        "| Sections | ${files.sumOf { f -> f.lines.count { isHeader(it) } }} |",
        "| Items | ${items.size} |",
        "| Words | ${items.sumOf { itemWordCount(it.content) }} |",
        "| Tags | ${files.sumOf { tagHits(it.lines, listPrefix).size }} |",
        "| Completion | $done/${boxed.size} ($pct) |",
    ).joinToString("\n")
}
