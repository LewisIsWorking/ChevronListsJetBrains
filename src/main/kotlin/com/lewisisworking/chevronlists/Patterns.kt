/**
 * Patterns.kt
 * Pure parsing logic for chevron-list markdown syntax. No IntelliJ Platform
 * imports - fully testable with plain JUnit. Mirrors the TypeScript implementation
 * in the ChevronLists VS Code extension.
 */
package com.lewisisworking.chevronlists

private val HEADER_REGEX   = Regex("""^> (.*)$""")
private val BULLET_REGEX   = Regex("""^(>+)\s+(\S)\s+(.*)$""")
private val NUMBERED_REGEX = Regex("""^(>+)\s+(\d+)\.\s*(.*)$""")
private val SUBHEAD_REGEX  = Regex("""^(#{1,6})\s+(.+)$""")

/** Result of parsing a chevron line */
data class ChevronHeader(val content: String)
data class ChevronBullet(val chevrons: String, val prefix: String, val content: String)
data class ChevronNumbered(val chevrons: String, val num: Int, val content: String)
data class ChevronSubheading(val level: Int, val content: String)

/** Pure: returns true if the line is a `> Section` header */
fun isHeader(text: String): Boolean = HEADER_REGEX.matches(text)

/** Pure: parses a `> Header` line, or null if the line is not a header */
fun parseHeader(text: String): ChevronHeader? =
    HEADER_REGEX.matchEntire(text)?.let { ChevronHeader(it.groupValues[1]) }

/** Pure: parses a `>> - item` bullet line, given the configured bullet prefix */
fun parseBullet(text: String, prefix: String): ChevronBullet? {
    val m = BULLET_REGEX.matchEntire(text) ?: return null
    if (m.groupValues[2] != prefix) return null
    return ChevronBullet(m.groupValues[1], m.groupValues[2], m.groupValues[3])
}

/** Pure: parses a `>> 1. item` numbered line */
fun parseNumbered(text: String): ChevronNumbered? {
    val m = NUMBERED_REGEX.matchEntire(text) ?: return null
    return ChevronNumbered(m.groupValues[1], m.groupValues[2].toInt(), m.groupValues[3])
}

/** Pure: parses a markdown `## Subheading` line. Does NOT match `> ` chevron headers. */
fun parseSubheading(text: String): ChevronSubheading? {
    if (isHeader(text)) return null
    val m = SUBHEAD_REGEX.matchEntire(text) ?: return null
    return ChevronSubheading(m.groupValues[1].length, m.groupValues[2].trim())
}