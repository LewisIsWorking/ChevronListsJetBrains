/**
 * Diagnostics.kt
 * Pure diagnostic logic for chevron-list markdown files. Mirrors the VS Code
 * `diagnostics.ts` collectIssues() function. No IntelliJ Platform imports.
 */
package com.lewisisworking.chevronlists

enum class IssueKind { DUPLICATE_HEADER, DUPLICATE_SUBHEADING, BAD_NUMBERING, EMPTY_SECTION }

/** A single diagnostic finding tied to a specific line in the document */
data class DiagnosticIssue(
    val line:    Int,
    val message: String,
    val kind:    IssueKind
)

private val ITEM_LINE_REGEX = Regex("""^>{2,}\s""")

/** Input for computeAutoFixEdits: a line of text and its 0-based line index */
data class AutoFixLine(val text: String, val lineIndex: Int)

/** An edit instruction: replace the given line entirely with newText */
data class AutoFixEdit(val lineIndex: Int, val newText: String)

/** Pure: returns all diagnostic issues in the document, sorted by line */
fun collectIssues(lines: List<String>, prefix: String): List<DiagnosticIssue> {
    val all = mutableListOf<DiagnosticIssue>()
    all += collectDuplicateHeaders(lines)
    all += collectDuplicateSubheadings(lines)
    all += collectBadNumbering(lines)
    all += collectEmptySections(lines)
    return all.sortedBy { it.line }
}

/** Pure: flags any `> Section` header whose name has been used earlier in the document */
fun collectDuplicateHeaders(lines: List<String>): List<DiagnosticIssue> {
    val seen = HashMap<String, Int>()
    val out  = mutableListOf<DiagnosticIssue>()
    for ((i, line) in lines.withIndex()) {
        val h = parseHeader(line) ?: continue
        val key   = h.content.trim().lowercase()
        val first = seen[key]
        if (first != null) {
            out += DiagnosticIssue(i, """Duplicate section name "${h.content}" (first at line ${first + 1})""", IssueKind.DUPLICATE_HEADER)
        } else {
            seen[key] = i
        }
    }
    return out
}

/** Pure: flags any `## Subheading` whose text has been used earlier (case-insensitive) */
fun collectDuplicateSubheadings(lines: List<String>): List<DiagnosticIssue> {
    val seen = HashMap<String, Int>()
    val out  = mutableListOf<DiagnosticIssue>()
    for ((i, line) in lines.withIndex()) {
        val s = parseSubheading(line) ?: continue
        val key   = s.content.lowercase()
        val first = seen[key]
        if (first != null) {
            out += DiagnosticIssue(i, """Duplicate subheading "${s.content}" (first at line ${first + 1})""", IssueKind.DUPLICATE_SUBHEADING)
        } else {
            seen[key] = i
        }
    }
    return out
}

/**
 * Pure: flags numbered items that break sequence within their section.
 * Items are grouped by (section, chevron-depth) so lists in different
 * sections or at different depths never collide.
 */
fun collectBadNumbering(lines: List<String>): List<DiagnosticIssue> {
    data class Item(val lineIndex: Int, val num: Int)
    val byKey = HashMap<String, MutableList<Item>>()
    var currentSection = -1
    for ((i, line) in lines.withIndex()) {
        if (isHeader(line)) { currentSection = i; continue }
        val n = parseNumbered(line) ?: continue
        val key = "$currentSection::${n.chevrons}"
        byKey.getOrPut(key) { mutableListOf() }.add(Item(i, n.num))
    }
    val out = mutableListOf<DiagnosticIssue>()
    for ((_, items) in byKey) {
        if (items.isEmpty()) continue
        var expected = items.first().num
        for (item in items) {
            if (item.num != expected) {
                out += DiagnosticIssue(item.lineIndex, "Numbering breaks here \u2014 expected $expected, got ${item.num}.", IssueKind.BAD_NUMBERING)
            }
            expected = item.num + 1
        }
    }
    return out
}

/**
 * Pure: computes the buffer edits needed to renumber broken numbered-list sequences.
 * Mirrors the VS Code `computeAutoFixEdits` in patternsUtils.ts. Items are grouped by
 * (section, chevron-depth) so lists in different sections never collide.
 *
 * For each group, the first item's number is taken as the start. The next items are
 * expected to be sequential; once a break is detected, everything from that point on
 * is renumbered.
 */
fun computeAutoFixEdits(lines: List<AutoFixLine>): List<AutoFixEdit> {
    data class Item(val lineIndex: Int, val num: Int, val text: String)
    val byKey = HashMap<String, MutableList<Item>>()
    var currentSection = -1
    for (l in lines) {
        if (isHeader(l.text)) { currentSection = l.lineIndex; continue }
        val n = parseNumbered(l.text) ?: continue
        val key = "$currentSection::${n.chevrons}"
        byKey.getOrPut(key) { mutableListOf() }.add(Item(l.lineIndex, n.num, l.text))
    }

    val edits = mutableListOf<AutoFixEdit>()
    for ((_, items) in byKey) {
        if (items.isEmpty()) continue
        var lastNum: Int? = null
        var breakIndex = -1
        for ((i, item) in items.withIndex()) {
            if (lastNum != null && item.num != lastNum + 1) { breakIndex = i; break }
            lastNum = item.num
        }
        if (breakIndex < 0) continue
        var counter = (lastNum ?: 0) + 1
        for (j in breakIndex until items.size) {
            val toFix = items[j]
            val m     = parseNumbered(toFix.text) ?: continue
            edits += AutoFixEdit(toFix.lineIndex, "${m.chevrons} $counter. ${m.content}")
            counter++
        }
    }
    return edits
}

/** Pure: flags any `> Section` that has no chevron item lines before the next section */
fun collectEmptySections(lines: List<String>): List<DiagnosticIssue> {
    val out = mutableListOf<DiagnosticIssue>()
    var currentSection = -1
    var hasContent     = false
    for ((i, line) in lines.withIndex()) {
        if (isHeader(line)) {
            if (currentSection >= 0 && !hasContent) {
                out += DiagnosticIssue(currentSection, "Empty section \u2014 no items.", IssueKind.EMPTY_SECTION)
            }
            currentSection = i
            hasContent     = false
        } else if (ITEM_LINE_REGEX.containsMatchIn(line)) {
            hasContent = true
        }
    }
    return out
}