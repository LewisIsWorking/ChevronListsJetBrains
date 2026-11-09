/**
 * SectionTools.kt
 * Pure logic for section colours, the section growth chart, searching items and
 * sections, and comparing two sections. No IntelliJ Platform imports. Mirrors
 * sectionColourCommands.ts, sectionGrowthCommands.ts, searchCommands.ts and
 * compareStatsCommands.ts in the VS Code extension.
 */
package com.lewisisworking.chevronlists

import kotlin.math.roundToInt

private val ANY_COLOUR_TAG   = Regex("""\s*\[colour:[^\]]+]""", RegexOption.IGNORE_CASE)
private val VALID_COLOUR_TAG = Regex("""\[colour:(red|green|blue|yellow|orange|purple)]""", RegexOption.IGNORE_CASE)
private const val GROWTH_CELLS = 10

/** Pure: the colour a header's `[colour:x]` tag names, or null */
fun sectionColourOf(header: String): ColourLabel? {
    val name = VALID_COLOUR_TAG.find(header)?.groupValues?.get(1)?.lowercase() ?: return null
    return ColourLabel.values().firstOrNull { it.displayName == name }
}

/** Pure: [header] with its colour tag replaced by [colour], or removed when that is null */
fun withSectionColour(header: String, colour: ColourLabel?): String {
    val stripped = ANY_COLOUR_TAG.replace(header, "").trim()
    return if (colour == null) stripped else "$stripped [colour:${colour.displayName}]"
}

/** Pure: a section's name without its word goal or colour tag */
fun plainSectionName(lines: List<String>, header: Int): String = ANY_COLOUR_TAG.replace(headerName(lines, header), "").trim()

/** Pure: the name of every section, in file order */
fun sectionNames(lines: List<String>): List<String> = lines.indices.filter { isHeader(lines[it]) }.map { plainSectionName(lines, it) }

/** Pure: the header line of the first section called [name], ignoring case */
fun sectionNamed(lines: List<String>, name: String): Int? =
    lines.indices.firstOrNull { isHeader(lines[it]) && plainSectionName(lines, it).equals(name.trim(), ignoreCase = true) }

/** Pure: a bar and item count per section, fullest first */
fun sectionGrowth(lines: List<String>, listPrefix: String): List<ReportEntry> {
    val sections = fileStats(lines, listPrefix, "").sections.sortedByDescending { it.items }
    val most     = (sections.maxByOrNull { it.items }?.items ?: 0).coerceAtLeast(1)
    return sections.map { s ->
        val filled = (s.items * GROWTH_CELLS.toDouble() / most).roundToInt()
        ReportEntry(s.line, "${"█".repeat(filled)}${"░".repeat(GROWTH_CELLS - filled)} ${s.items}  ${plainSectionName(lines, s.line)}")
    }
}

/** Pure: every item in the file with the section it is in */
fun itemSearchEntries(lines: List<String>, listPrefix: String): List<ReportEntry> =
    foundItems(lines, listPrefix).map { ReportEntry(it.line, it.content + it.suffix) }

/** Pure: every section in the file */
fun sectionSearchEntries(lines: List<String>): List<ReportEntry> =
    lines.indices.filter { isHeader(lines[it]) }.map { ReportEntry(it, plainSectionName(lines, it)) }

private fun tableCell(text: String) = text.replace("|", "\\|")

/** Pure: a markdown table comparing the sections headed at [first] and [second] */
fun compareSections(lines: List<String>, first: Int, second: Int, listPrefix: String): String {
    val a = sectionStats(lines, first, listPrefix, "")
    val b = sectionStats(lines, second, listPrefix, "")
    return listOf(
        "| Metric | ${tableCell(plainSectionName(lines, first))} | ${tableCell(plainSectionName(lines, second))} |",
        "|--------|--------|--------|",
        "| Items | ${a.items} | ${b.items} |",
        "| Words | ${a.words} | ${b.words} |",
        "| Done | ${a.done}/${a.checks} | ${b.done}/${b.checks} |",
        "| Tags | ${a.tags} | ${b.tags} |",
    ).joinToString("\n")
}
