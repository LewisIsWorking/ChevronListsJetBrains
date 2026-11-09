/**
 * Kanban.kt
 * Pure logic for Show Kanban: sorts a file's items into Todo, In Progress and
 * Done columns. A checked `[x]` item is done; a starred one (the ⭐ marker, or
 * VS Code's leading `* `) is in progress; anything else is to do. No IntelliJ
 * Platform imports. Mirrors kanbanCommands.ts in the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** One item on the board: its text without checkbox or star, its section and its line */
data class KanbanCard(val content: String, val section: String, val line: Int)

/** The three columns of the board */
data class KanbanBoard(val todo: List<KanbanCard>, val inProgress: List<KanbanCard>, val done: List<KanbanCard>)

private const val STAR   = "⭐"

/** Pure: every non-empty item in the file, sorted into its column in document order */
fun kanbanBoard(lines: List<String>, listPrefix: String): KanbanBoard {
    val todo = mutableListOf<KanbanCard>(); val inProgress = mutableListOf<KanbanCard>(); val done = mutableListOf<KanbanCard>()
    var section = ""
    for ((i, text) in lines.withIndex()) {
        val header = parseHeader(text)
        if (header != null) { section = header.content.trim(); continue }
        val content = (parseNumbered(text)?.content ?: parseBullet(text, listPrefix)?.content)?.trim() ?: continue
        val parts   = splitItem(content)
        val starred = isStarred(parts)
        val plain   = joinItem(parts.copy(check = "", marker = if (parts.marker == "*") "" else parts.marker))
            .split(Regex("""\s+""")).filter { it.isNotEmpty() && it != STAR }.joinToString(" ")
        if (plain.isEmpty()) continue
        val card = KanbanCard(plain, section, i)
        when {
            parts.check.equals("[x]", ignoreCase = true) -> done += card
            starred -> inProgress += card
            else    -> todo += card
        }
    }
    return KanbanBoard(todo, inProgress, done)
}
