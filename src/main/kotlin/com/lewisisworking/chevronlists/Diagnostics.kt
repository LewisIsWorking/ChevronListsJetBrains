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
 * Pure: flags numbered items that break sequence within their list.
 * Items are grouped into lists by NumberingRuns, so lists in different
 * sections, at different depths, or under different parents never collide.
 */
fun collectBadNumbering(lines: List<String>): List<DiagnosticIssue> {
    data class Item(val lineIndex: Int, val num: Int)
    val byKey = LinkedHashMap<Int, MutableList<Item>>()
    val runs  = NumberingRuns()
    for ((i, line) in lines.withIndex()) {
        val run = runs.visit(line) ?: continue
        byKey.getOrPut(run) { mutableListOf() }.add(Item(i, parseNumbered(line)!!.num))
    }
    val out = mutableListOf<DiagnosticIssue>()
    for ((_, items) in byKey) {
        if (items.isEmpty()) continue
        var expected = items.first().num
        for (item in items) {
            if (item.num != expected) {
                out += DiagnosticIssue(item.lineIndex, "Numbering breaks here: expected $expected, got ${item.num}.", IssueKind.BAD_NUMBERING)
            }
            expected = item.num + 1
        }
    }
    return out
}

/**
 * Pure: computes the buffer edits needed to renumber broken numbered-list sequences.
 * Mirrors the VS Code `computeAutoFixEdits` in patternsUtils.ts. Items are grouped
 * into lists by NumberingRuns, so the children of two parents are two lists.
 *
 * For each group, the first item's number is taken as the start. The next items are
 * expected to be sequential; once a break is detected, everything from that point on
 * is renumbered.
 */
fun computeAutoFixEdits(lines: List<AutoFixLine>): List<AutoFixEdit> {
    data class Item(val lineIndex: Int, val num: Int, val text: String)
    val byKey = LinkedHashMap<Int, MutableList<Item>>()
    val runs  = NumberingRuns()
    for (l in lines) {
        val run = runs.visit(l.text) ?: continue
        byKey.getOrPut(run) { mutableListOf() }.add(Item(l.lineIndex, parseNumbered(l.text)!!.num, l.text))
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
                out += DiagnosticIssue(currentSection, "Empty section: no items.", IssueKind.EMPTY_SECTION)
            }
            currentSection = i
            hasContent     = false
        } else if (ITEM_LINE_REGEX.containsMatchIn(line)) {
            hasContent = true
        }
    }
    return out
}