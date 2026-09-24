/**
 * NumberingRuns.kt
 * Which numbered list ("run") each numbered item belongs to. No IntelliJ
 * Platform imports. Mirrors NumberingRuns in the VS Code extension.
 *
 * Numbered items at the same depth continue one list until a header, or a
 * chevron line SHALLOWER than them, interrupts it. So the children of two
 * different parents are two lists, each starting at 1, as in any outline:
 *
 *     >> 1. a
 *     >>> 1. a1
 *     >> 2. b
 *     >>> 1. b1     <- a new list, not "2."
 *
 * Numbering used to be keyed by depth alone across the whole section, so the
 * checker flagged b1 and auto-fix renumbered it to 2 as the user typed.
 */
package com.lewisisworking.chevronlists

private val CHEVRON_DEPTH_REGEX = Regex("""^(>{2,}) \S""")

/** A line's chevron depth (">>" is 2), or null for a line that is not a chevron line */
fun chevronDepth(text: String): Int? = CHEVRON_DEPTH_REGEX.find(text)?.groupValues?.get(1)?.length

/** Feed every line in document order to [visit] */
class NumberingRuns {
    private val runs = HashMap<Int, Int>()
    private var nextRun = 0

    /** Records [text] and returns the run of the numbered item on it, or null when it is not one */
    fun visit(text: String): Int? {
        if (isHeader(text)) { runs.clear(); return null }
        val depth = chevronDepth(text) ?: return null
        runs.keys.removeAll { it > depth }
        if (parseNumbered(text) == null) return null
        return runs.getOrPut(depth) { nextRun++ }
    }
}
