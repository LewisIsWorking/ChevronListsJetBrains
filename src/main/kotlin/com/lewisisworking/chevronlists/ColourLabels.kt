/**
 * ColourLabels.kt
 * Pure logic for the 6 colour-label tokens (red, green, blue, yellow, orange,
 * purple) ported from the VS Code Chevron Lists extension. Labels are written
 * inside item content as `{red}`, `{green}`, etc. and can be set, removed, or
 * collected for filtering.
 *
 * No IntelliJ Platform imports - fully testable.
 */
package com.lewisisworking.chevronlists

/** A colour label with its display name and the hex used for visual highlighting */
enum class ColourLabel(val displayName: String, val hex: String) {
    RED   ("red",    "#E06C75"),
    GREEN ("green",  "#98C379"),
    BLUE  ("blue",   "#61AFEF"),
    YELLOW("yellow", "#E5C07B"),
    ORANGE("orange", "#FF9800"),
    PURPLE("purple", "#A052E5");

    /** The `{name}` token string used in item content */
    val token: String get() = "{$displayName}"
}

private val COLOUR_LABEL_REGEX = Regex("""\{(red|green|blue|yellow|orange|purple)\}""")
private val MULTI_SPACE_REGEX  = Regex("""\s{2,}""")

/** Pure: returns the first colour label found in the content, or null */
fun parseColourLabel(content: String): ColourLabel? {
    val match = COLOUR_LABEL_REGEX.find(content) ?: return null
    return ColourLabel.values().firstOrNull { it.displayName == match.groupValues[1] }
}

/** Pure: strips ALL colour labels from content, collapsing extra whitespace */
fun removeColourLabel(content: String): String =
    content.replace(COLOUR_LABEL_REGEX, "").replace(MULTI_SPACE_REGEX, " ").trim()

/** Pure: replaces any existing colour label and prepends the given one */
fun setColourLabel(content: String, label: ColourLabel): String {
    val stripped = removeColourLabel(content)
    return if (stripped.isEmpty()) label.token else "${label.token} $stripped"
}

/** Pure: returns line-local IntRanges and labels of every {colour} occurrence in a line */
fun findColourLabels(line: String): List<Pair<IntRange, ColourLabel>> =
    COLOUR_LABEL_REGEX.findAll(line).mapNotNull { match ->
        val label = ColourLabel.values().firstOrNull { it.displayName == match.groupValues[1] }
            ?: return@mapNotNull null
        match.range to label
    }.toList()

/**
 * Pure: line-level wrapper. Sets the colour label on a chevron item, preserving
 * its chevrons + prefix/number. Returns null if the line is not an item.
 */
fun computeSetColourLabel(line: String, listPrefix: String, label: ColourLabel): String? {
    val b = parseBullet(line, listPrefix)
    if (b != null) return "${b.chevrons} ${b.prefix} ${setColourLabel(b.content, label)}"
    val n = parseNumbered(line)
    if (n != null) return "${n.chevrons} ${n.num}. ${setColourLabel(n.content, label)}"
    return null
}

/**
 * Pure: line-level wrapper. Removes any colour label from a chevron item.
 * Returns null if the line is not an item, or if the line has no label to remove.
 */
fun computeRemoveColourLabel(line: String, listPrefix: String): String? {
    val b = parseBullet(line, listPrefix)
    if (b != null) {
        val stripped = removeColourLabel(b.content)
        if (stripped == b.content) return null
        return "${b.chevrons} ${b.prefix} $stripped"
    }
    val n = parseNumbered(line)
    if (n != null) {
        val stripped = removeColourLabel(n.content)
        if (stripped == n.content) return null
        return "${n.chevrons} ${n.num}. $stripped"
    }
    return null
}