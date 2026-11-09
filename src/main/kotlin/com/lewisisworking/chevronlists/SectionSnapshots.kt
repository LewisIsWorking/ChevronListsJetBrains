/**
 * SectionSnapshots.kt
 * Pure logic for archiving a whole section, reviewing the archive and saving or
 * restoring snapshots. No IntelliJ Platform imports - fully testable with plain
 * JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

private val WHITESPACE_RUN = Regex("""\s+""")

private fun contentEnd(lines: List<String>, header: Int): Int =
    (sectionLastLine(lines, header) downTo header).first { it == header || lines[it].isNotBlank() }

/**
 * Pure: the document with the caret's section moved to the archive, straight
 * after the archive's items. Null outside a section, or when the section is the
 * archive or already sits directly after it.
 */
fun archiveSection(lines: List<String>, caretLine: Int): List<String>? {
    val header  = headerAtOrAbove(lines, caretLine) ?: return null
    val archive = lines.indexOfFirst(::isArchiveHeader)
    var top = header
    while (top > 0 && lines[top - 1].isBlank()) top--
    if (archive in 0..header && contentEnd(lines, archive) + 1 >= top) return null

    val last    = sectionLastLine(lines, header)
    val section = lines.subList(header, contentEnd(lines, header) + 1)
    val rest    = (lines.take(header) + lines.drop(last + 1)).toMutableList()
    if (archive >= 0) {
        val moved = if (archive > header) archive - (last - header + 1) else archive
        rest.addAll(contentEnd(rest, moved) + 1, section)
    } else {
        val lastContent = rest.indexOfLast { it.isNotBlank() }
        val block = (if (lastContent >= 0) listOf("") else emptyList()) + "> Archive" + section
        rest.addAll(lastContent + 1, block)
    }
    return rest
}

/** Pure: every item of the archive, indented by depth; null when the file has no archive */
fun archiveEntries(lines: List<String>, listPrefix: String): List<ReportEntry>? {
    val archive = lines.indexOfFirst(::isArchiveHeader).takeIf { it >= 0 } ?: return null
    return (archive + 1..sectionLastLine(lines, archive)).mapNotNull { line ->
        val content  = itemContent(lines[line], listPrefix) ?: return@mapNotNull null
        val chevrons = parseNumbered(lines[line])?.chevrons ?: parseBullet(lines[line], listPrefix)!!.chevrons
        ReportEntry(line, "  ".repeat(chevrons.length - 2) + content.take(70))
    }
}

/** Pure: the text of the caret's section to save as a snapshot; null outside a section */
fun sectionSnapshotText(lines: List<String>, caretLine: Int): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    return lines.subList(header, sectionLastLine(lines, header) + 1).joinToString("\n")
}

/** Pure: the document with the caret's section replaced by [snapshot]; null outside a section */
fun restoreSnapshotText(lines: List<String>, caretLine: Int, snapshot: String): List<String>? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    return lines.take(header) + snapshot.split("\n") + lines.drop(sectionLastLine(lines, header) + 1)
}

/** Pure: a document listing saved snapshots as "name (saved at)" */
fun snapshotListing(snapshots: List<Pair<String, String>>): String =
    (listOf("# Snapshots", "") + snapshots.map { (name, savedAt) -> "- $name ($savedAt)" }).joinToString("\n") + "\n"

private fun wordsOf(text: String) = text.split(WHITESPACE_RUN).filter { it.isNotEmpty() }.toSet()

/** Pure: a document comparing an item with its snapshot, word by word */
fun itemSnapshotDiff(before: String, after: String): String {
    val old     = wordsOf(before)
    val new     = wordsOf(after)
    val removed = (old - new).map { "- $it" }
    val added   = (new - old).map { "+ $it" }
    val changes = (removed + added).ifEmpty { listOf("_No changes detected_") }.joinToString("\n")
    return "# Item Snapshot Diff\n\n## Before\n\n$before\n\n## After\n\n$after\n\n## Changes\n\n$changes"
}
