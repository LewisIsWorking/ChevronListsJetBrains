/**
 * PasteItems.kt
 * Pure logic for pasting several lines into a chevron item: each line becomes its
 * own item at the same depth. No IntelliJ Platform imports. Mirrors
 * `itemsFromPaste` in the VS Code extension's pasteItemsProvider.ts.
 */
package com.lewisisworking.chevronlists

private val LINE_BREAK = Regex("""\r?\n""")

/**
 * Pure: the text that makes each pasted line its own item, when [clip] is pasted
 * at [start] of [lineText] (a selection within the line is replaced from there).
 * Null when this is not such a paste: the line is not an item, the paste point is
 * before the item's content, or there is only one non-blank line.
 *
 * The first line goes into the item at the paste point. Each further line becomes
 * a new item at the same depth: bullets use [listPrefix], numbered items continue
 * the number (the auto-fix then renumbers the items after them). Blank lines are
 * dropped and each line is trimmed, so indentation from chat logs and e-mails
 * goes. A line that is already a chevron item keeps only its text.
 */
fun itemsFromPaste(lineText: String, start: Int, clip: String, listPrefix: String): String? {
    val numbered = parseNumbered(lineText)
    val bullet   = if (numbered == null) parseBullet(lineText, listPrefix) else null
    val content  = numbered?.content ?: bullet?.content ?: return null
    if (start < lineText.length - content.length) return null

    val lines = clip.split(LINE_BREAK)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { parseBullet(it, listPrefix)?.content ?: parseNumbered(it)?.content ?: it }
    if (lines.size < 2) return null

    val rest = lines.drop(1).mapIndexed { i, l ->
        if (numbered != null) "${numbered.chevrons} ${numbered.num + i + 1}. $l"
        else "${bullet!!.chevrons} $listPrefix $l"
    }
    return (listOf(lines[0]) + rest).joinToString("\n")
}
