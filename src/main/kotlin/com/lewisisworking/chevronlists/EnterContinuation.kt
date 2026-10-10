/**
 * EnterContinuation.kt
 * Pure logic for deciding what should happen when Enter is pressed on a
 * chevron-list line. No IntelliJ Platform imports - fully testable.
 *
 * Mirrors the VS Code `enterHandler.ts` behaviour:
 *   - On `> Header`        -> insert `>> - ` (or `>> 1. ` for ordered)
 *   - On `>> - content`    -> insert `>> - `
 *   - On `>> N. content`   -> insert `>> N+1. `
 *   - On empty list item   -> end the list (clear line, fall through to default Enter)
 *   - On anything else     -> let the default Enter handler run
 */
package com.lewisisworking.chevronlists

/** What ChevronEnterHandler should do when Enter is pressed */
sealed class EnterAction {
    /** No special handling - let the IDE process Enter normally */
    object Default : EnterAction()
    /** The current line is an empty list item - clear it and fall through to default */
    object EndList : EnterAction()
    /** Insert a newline followed by this text at the caret */
    /** Insert a newline followed by this text at the caret; [renumber] renumbers the section after */
    data class Continue(val insert: String, val renumber: Boolean = false) : EnterAction()
}

/**
 * Pure: decides what to do when Enter is pressed on a line.
 *
 * @param line              The full text of the line the caret is on
 * @param listPrefix        Bullet prefix character, typically "-"
 * @param defaultNewListType Either "unordered" (default `>> - `) or "ordered" (`>> 1. `)
 * @param nextLine          The line below; on a header with a list under it, Enter continues that list's type
 */
fun computeEnterAction(line: String, listPrefix: String, defaultNewListType: String, nextLine: String = ""): EnterAction {
    // > Header -> start a new list at depth 2, matching a list already under it
    if (isHeader(line)) {
        val nextNumbered = parseNumbered(nextLine)
        if (nextNumbered != null && nextNumbered.chevrons.length == 2) return EnterAction.Continue(">> 1. ", renumber = true)
        val nextBullet = parseBullet(nextLine, listPrefix)
        if (nextBullet != null && nextBullet.chevrons.length == 2) return EnterAction.Continue(">> ${nextBullet.prefix} ")
        val first = if (defaultNewListType == "ordered") "1." else listPrefix
        return EnterAction.Continue(">> $first ")
    }

    // >> N. content -> continue with the next number
    val n = parseNumbered(line)
    if (n != null) {
        return if (n.content.isBlank())
            EnterAction.EndList
        else
            EnterAction.Continue("${n.chevrons} ${n.num + 1}. ", renumber = parseNumbered(nextLine)?.chevrons == n.chevrons)
    }

    // >> - content -> continue with another bullet
    val b = parseBullet(line, listPrefix)
    if (b != null) {
        return if (b.content.isBlank())
            EnterAction.EndList
        else
            EnterAction.Continue("${b.chevrons} ${b.prefix} ")
    }

    return EnterAction.Default
}