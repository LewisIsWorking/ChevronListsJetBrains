/**
 * ArchiveDone.kt
 * Pure logic for Archive Done Items: moves every done item of the section at
 * the caret, with everything nested under it, to the end of a `> Archive`
 * section (created at the end of the file when there is none). No IntelliJ
 * Platform imports.
 *
 * A done item moves as a block, so its children go with it rather than being
 * left behind under whichever item sits above them.
 */
package com.lewisisworking.chevronlists

private val DONE_BOX = Regex("""^\[[xX]]""")

private fun depthOf(line: String, listPrefix: String): Int? =
    parseNumbered(line)?.chevrons?.length ?: parseBullet(line, listPrefix)?.chevrons?.length

private fun isDone(line: String, listPrefix: String): Boolean {
    val content = parseNumbered(line)?.content ?: parseBullet(line, listPrefix)?.content ?: return false
    return DONE_BOX.containsMatchIn(content.trimStart())
}

private fun isArchiveHeader(line: String): Boolean =
    parseHeader(line)?.content?.trim()?.equals("archive", ignoreCase = true) == true

/** The result of an archive: the new document, and how many done items moved */
data class ArchiveResult(val lines: List<String>, val archived: Int)

/**
 * Pure: the document with the done items of the section at [caretLine] moved
 * to the archive. Null when there is nothing to archive, or the caret is in the
 * archive itself.
 */
fun computeArchiveDone(lines: List<String>, caretLine: Int, listPrefix: String): ArchiveResult? {
    val body = sectionBody(lines, caretLine) ?: return null
    val header = body.first - 1
    if (header >= 0 && isArchiveHeader(lines[header])) return null

    // Collect done blocks; a done child inside a block already taken goes with it
    val taken = sortedSetOf<Int>()
    var archived = 0
    var i = body.first
    while (i <= body.last) {
        val depth = depthOf(lines[i], listPrefix)
        if (depth == null || !isDone(lines[i], listPrefix)) { i++; continue }
        var end = i
        while (end + 1 <= body.last && (depthOf(lines[end + 1], listPrefix) ?: 0) > depth) end++
        (i..end).forEach { taken += it }
        archived++
        i = end + 1
    }
    if (archived == 0) return null

    val moved = taken.map { lines[it] }
    val rest = lines.filterIndexed { index, _ -> index !in taken }.toMutableList()

    val archiveHeader = rest.indexOfFirst { isArchiveHeader(it) }
    if (archiveHeader >= 0) {
        // After the archive's last non-blank line, before the blank lines that end it
        var end = archiveHeader
        for (j in archiveHeader + 1 until rest.size) {
            if (isHeader(rest[j])) break
            if (rest[j].isNotBlank()) end = j
        }
        rest.addAll(end + 1, moved)
    } else {
        // A new archive after the last non-blank line, separated by a blank line
        val lastContent = rest.indexOfLast { it.isNotBlank() }
        val block = (if (lastContent >= 0) listOf("") else emptyList()) + "> Archive" + moved
        rest.addAll(lastContent + 1, block)
    }
    return ArchiveResult(rest, archived)
}

/** A text edit: replace [start, end) of the old text with [text] */
data class TextReplacement(val start: Int, val end: Int, val text: String)

/**
 * Pure: the smallest single replacement that turns [before] into [after], from
 * their common prefix and suffix. Null when they are equal. Lets an action
 * rewrite only what changed, even when the number of lines changes.
 */
fun minimalReplacement(before: String, after: String): TextReplacement? {
    if (before == after) return null
    var prefix = 0
    val max = minOf(before.length, after.length)
    while (prefix < max && before[prefix] == after[prefix]) prefix++
    var suffix = 0
    while (suffix < max - prefix && before[before.length - 1 - suffix] == after[after.length - 1 - suffix]) suffix++
    return TextReplacement(prefix, before.length - suffix, after.substring(prefix, after.length - suffix))
}
