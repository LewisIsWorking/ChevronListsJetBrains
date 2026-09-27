/**
 * DuplicateItem.kt
 * Pure logic for duplicating the item at the caret. No IntelliJ Platform imports.
 *
 * The copy goes after the item's whole block (the item and everything nested
 * under it), so it never lands between the item and its children, and the
 * children are copied with it. A numbered copy takes the next number, and the
 * later items of the same list move up by one, so "1, 2, 3" duplicated at 1
 * reads "1, 2, 3, 4". The VS Code extension fixed both behaviours in 26.7.0.
 */
package com.lewisisworking.chevronlists

/**
 * A duplication as edits against the original document: [insert] goes after
 * line [afterLine], and each line in [renumbered] is replaced by its new text.
 * The copy's first line ends up at [afterLine] + 1.
 */
data class DuplicateEdit(val afterLine: Int, val insert: List<String>, val renumbered: Map<Int, String>)

private fun itemDepth(line: String, listPrefix: String): Int? =
    parseNumbered(line)?.chevrons?.length ?: parseBullet(line, listPrefix)?.chevrons?.length

/** Pure: how to duplicate the item at [index], or null when the line is not an item */
fun computeDuplicateItem(lines: List<String>, index: Int, listPrefix: String): DuplicateEdit? {
    if (index !in lines.indices) return null
    val depth = itemDepth(lines[index], listPrefix) ?: return null

    // The block: the item plus the deeper items directly under it
    var end = index
    while (end + 1 < lines.size && (itemDepth(lines[end + 1], listPrefix) ?: 0) > depth) end++

    val copy = lines.subList(index, end + 1).toMutableList()
    val renumbered = LinkedHashMap<Int, String>()

    val numbered = parseNumbered(lines[index])
    if (numbered != null) {
        copy[0] = "${numbered.chevrons} ${numbered.num + 1}. ${numbered.content}"
        // Later items of the same list move up by one. The list ends at a header
        // or at a chevron line shallower than it; deeper lines belong to children.
        for (i in end + 1 until lines.size) {
            val text = lines[i]
            if (isHeader(text)) break
            val d = chevronDepth(text) ?: continue
            if (d < depth) break
            if (d > depth) continue
            val n = parseNumbered(text) ?: continue
            renumbered[i] = "${n.chevrons} ${n.num + 1}. ${n.content}"
        }
    }
    return DuplicateEdit(end, copy, renumbered)
}

/** Pure: [edit] applied to [lines] (what the editor ends up showing) */
fun applyDuplicate(lines: List<String>, edit: DuplicateEdit): List<String> {
    val out = lines.mapIndexed { i, text -> edit.renumbered[i] ?: text }.toMutableList()
    out.addAll(edit.afterLine + 1, edit.insert)
    return out
}
