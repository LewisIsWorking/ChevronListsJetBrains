/**
 * ItemCommands.kt
 * Pure logic for item-level transformations (toggle done, etc.).
 * No IntelliJ Platform imports - fully testable.
 */
package com.lewisisworking.chevronlists

/**
 * Pure: cycles a checkbox marker at the start of an item's content.
 *   no checkbox -> "[x] ..."
 *   "[ ]"       -> "[x] ..."
 *   "[x]"       -> "[ ] ..."
 *   "[]"        -> "[x] ..."
 */
fun toggleCheckbox(content: String): String {
    val trimmed = content.trimStart()
    return when {
        trimmed.startsWith("[x]") || trimmed.startsWith("[X]") ->
            "[ ] " + trimmed.substring(3).trimStart()
        trimmed.startsWith("[ ]") ->
            "[x] " + trimmed.substring(3).trimStart()
        trimmed.startsWith("[]") ->
            "[x] " + trimmed.substring(2).trimStart()
        else ->
            "[x] " + trimmed
    }
}

/**
 * Pure: returns the new line text after toggling its done state, or null
 * if the line is not a chevron item.
 */
fun computeToggleDone(line: String, listPrefix: String): String? {
    val b = parseBullet(line, listPrefix)
    if (b != null) {
        return "${b.chevrons} ${b.prefix} ${toggleCheckbox(b.content)}"
    }
    val n = parseNumbered(line)
    if (n != null) {
        return "${n.chevrons} ${n.num}. ${toggleCheckbox(n.content)}"
    }
    return null
}