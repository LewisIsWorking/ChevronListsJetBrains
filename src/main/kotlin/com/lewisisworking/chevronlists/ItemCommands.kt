/**
 * ItemCommands.kt
 * Pure logic for item-level transformations (toggle done, etc.).
 * No IntelliJ Platform imports - fully testable.
 */
package com.lewisisworking.chevronlists

/**
 * Pure: toggles a single-token marker (e.g. "⭐" or "📌") inside an item's content.
 * If the marker is present anywhere in the content (as a whitespace-separated token),
 * it is removed. Otherwise the marker is prepended.
 */
fun toggleMarker(content: String, marker: String): String {
    val parts = content.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
    return if (parts.contains(marker)) {
        parts.filterNot { it == marker }.joinToString(" ")
    } else {
        val rest = parts.joinToString(" ")
        if (rest.isEmpty()) marker else "$marker $rest"
    }
}

/**
 * Pure: returns the new line text after toggling the given marker on its item,
 * or null if the line is not a chevron item.
 */
fun computeToggleMarker(line: String, listPrefix: String, marker: String): String? {
    val b = parseBullet(line, listPrefix)
    if (b != null) {
        return "${b.chevrons} ${b.prefix} ${toggleMarker(b.content, marker)}"
    }
    val n = parseNumbered(line)
    if (n != null) {
        return "${n.chevrons} ${n.num}. ${toggleMarker(n.content, marker)}"
    }
    return null
}

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