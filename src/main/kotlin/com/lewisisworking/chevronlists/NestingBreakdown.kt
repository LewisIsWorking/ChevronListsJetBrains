/**
 * NestingBreakdown.kt
 * Pure logic for the nesting breakdown: how many items sit at each chevron
 * depth in a section (`>>` is depth 1, `>>>` depth 2, and so on). Mirrors the
 * VS Code extension's CL: Show Nesting Breakdown. No IntelliJ Platform imports.
 */
package com.lewisisworking.chevronlists

/** Pure: item counts by chevron depth (1-based) for the section headed at [header] */
fun nestingBreakdown(lines: List<String>, header: Int, listPrefix: String): Map<Int, Int> {
    val counts = sortedMapOf<Int, Int>()
    var i = header + 1
    while (i < lines.size && parseHeader(lines[i]) == null && parseSubheading(lines[i]) == null) {
        val chevrons = parseNumbered(lines[i])?.chevrons ?: parseBullet(lines[i], listPrefix)?.chevrons
        if (chevrons != null) {
            val depth = chevrons.length - 1
            counts[depth] = (counts[depth] ?: 0) + 1
        }
        i++
    }
    return counts
}

/** Pure: the dialog text for [name]'s breakdown, such as `"Paid" nesting breakdown` plus one line per depth */
fun nestingBreakdownText(name: String, counts: Map<Int, Int>): String {
    val parts = counts.entries.sortedBy { it.key }.map { (depth, count) ->
        "${"›".repeat(depth)} Depth $depth: $count ${if (count == 1) "item" else "items"}"
    }
    return "CL: \"$name\" nesting breakdown\n${parts.joinToString("\n")}"
}
