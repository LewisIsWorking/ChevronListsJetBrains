/**
 * DailyNotes.kt
 * Pure logic for daily notes: one `YYYY-MM-DD.md` file per day, created from a
 * template, plus "send to daily note", which files an item under `> Inbox`.
 * Ported from the VS Code extension's dailyNoteCommands.ts and
 * sendToDailyNoteCommands.ts.
 */
package com.lewisisworking.chevronlists

import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

const val INBOX_HEADER = "> Inbox"

/** The daily note's file name for [date], e.g. "2026-10-08.md" */
fun dailyNoteFileName(date: LocalDate): String = "$date.md"

/** Fills {{date}}, {{weekday}} and {{day}} in [template] with [date]'s values */
fun fillDateTemplate(template: String, date: LocalDate): String = template
    .replace("{{date}}", date.toString())
    .replace("{{weekday}}", date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
    .replace("{{day}}", date.dayOfMonth.toString())

/** A new daily note's text: the template filled in, or a dated header with one empty item */
fun newDailyNote(template: String, prefix: String, date: LocalDate): String {
    val raw = template.ifBlank { "> {{date}}\n>> $prefix " }
    return fillDateTemplate(raw, date) + "\n"
}

/**
 * The folder daily notes live in: [setting] when absolute, else [setting]
 * under [projectBase]; the project folder itself when [setting] is blank.
 * Null when a relative folder is needed but there is no project folder.
 */
fun dailyNotesFolder(setting: String, projectBase: String?): Path? {
    val folder = setting.trim()
    if (folder.isNotEmpty() && Paths.get(folder).isAbsolute) return Paths.get(folder)
    val base = projectBase ?: return null
    return if (folder.isEmpty()) Paths.get(base) else Paths.get(base, folder)
}

/** The first empty item line (e.g. ">> - "), where the caret goes in a new note; null if none */
fun firstBlankItemLine(lines: List<String>, prefix: String): Int? {
    val index = lines.indexOfFirst { Regex("^>> \\S+ $").matches(it) || it.endsWith("$prefix ") }
    return index.takeIf { it >= 0 }
}

/** The text of the item on [line], without its chevrons and marker; null when [line] is not an item */
fun itemContent(line: String, prefix: String): String? =
    (parseBullet(line, prefix)?.content ?: parseNumbered(line)?.content)?.takeIf { it.isNotEmpty() }

/**
 * [noteText] with `>> prefix content` filed first under its `> Inbox`
 * section. A note with no Inbox gets one at the end; a missing note
 * ([noteText] null) is created with a dated title.
 */
fun sendToInbox(noteText: String?, content: String, prefix: String, date: LocalDate): String {
    val item = ">> $prefix $content"
    if (noteText == null) return "# $date\n\n$INBOX_HEADER\n$item\n"
    val lines = noteText.split("\n").toMutableList()
    val inbox = lines.indexOfFirst { it.trim().equals(INBOX_HEADER, ignoreCase = true) }
    if (inbox >= 0) {
        lines.add(inbox + 1, item)
        return lines.joinToString("\n")
    }
    val base = if (noteText.isEmpty() || noteText.endsWith("\n")) noteText else noteText + "\n"
    val gap  = if (base.isEmpty()) "" else "\n"
    return "$base$gap$INBOX_HEADER\n$item\n"
}
