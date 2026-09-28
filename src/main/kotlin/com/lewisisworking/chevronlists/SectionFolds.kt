/**
 * SectionFolds.kt
 * Pure logic for folding `> Header` sections. No IntelliJ Platform imports.
 *
 * A section folds from the end of its header line to its last non-blank line,
 * so the blank line before the next header stays visible. The VS Code
 * extension folds the same sections (it keeps the trailing blank lines inside).
 */
package com.lewisisworking.chevronlists

/** A foldable section: its header line, its last non-blank line, and how many items it holds */
data class SectionFold(val headerLine: Int, val lastLine: Int, val items: Int)

/** Pure: one fold per section that has at least one non-blank line under its header */
fun sectionFolds(lines: List<String>, listPrefix: String): List<SectionFold> {
    val folds = mutableListOf<SectionFold>()
    var header = -1
    var last = -1
    var items = 0

    fun close() {
        if (header >= 0 && last > header) folds += SectionFold(header, last, items)
    }

    for ((i, text) in lines.withIndex()) {
        if (isHeader(text)) {
            close()
            header = i; last = i; items = 0
            continue
        }
        if (header < 0) continue
        if (text.isNotBlank()) last = i
        if (parseNumbered(text) != null || parseBullet(text, listPrefix) != null) items++
    }
    close()
    return folds
}

/** Pure: the text a folded section shows after its header, e.g. " (3 items)" */
fun foldPlaceholder(fold: SectionFold): String = when (fold.items) {
    0    -> " ..."
    1    -> " (1 item)"
    else -> " (${fold.items} items)"
}
