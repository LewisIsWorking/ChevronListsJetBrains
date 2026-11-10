/**
 * JumpHistory.kt
 * Pure logic of the jump history and recent section reports.
 */
package com.lewisisworking.chevronlists

/** The section each jump started in, newest first, each section once. Jumps from above every section are left out */
fun recentSectionEntries(lines: List<String>, jumpLinesNewestFirst: List<Int>): List<ReportEntry> =
    jumpLinesNewestFirst.mapNotNull { headerAtOrAbove(lines, it) }.distinct()
        .map { ReportEntry(it, "${plainSectionName(lines, it)}  (line ${it + 1})") }

/** One entry per jump, from (line, column) pairs newest first */
fun jumpHistoryEntries(positionsNewestFirst: List<Pair<Int, Int>>): List<ReportEntry> =
    positionsNewestFirst.mapIndexed { i, (line, column) ->
        val steps = if (i == 0) "most recent" else "${i + 1} steps back"
        ReportEntry(line, "Line ${line + 1}, Col ${column + 1}  ($steps)")
    }
