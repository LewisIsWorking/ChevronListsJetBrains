/**
 * TagRewrites.kt
 * Pure logic for renaming tags, changing a section's bullets, grouping items by
 * tag and setting a word goal. No IntelliJ Platform imports - fully testable
 * with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** The bullets Change Item Prefix offers */
val ITEM_PREFIXES = listOf("-", "*", "•")

private val ANY_TAG       = Regex("""(?<!\S)#(\w[\w-]*)""")
private val GROUP_HEADING = Regex("""^// #[\w-]+$""")
private val WORD_GOAL     = Regex("""\s*==(\d+)""")

private fun oneTag(tag: String) = Regex("""(?<!\S)#${Regex.escape(tag.removePrefix("#"))}(?![\w-])""", RegexOption.IGNORE_CASE)

private fun tagsOf(content: String) = ANY_TAG.findAll(content).map { it.groupValues[1].lowercase() }.toList()

private fun tagsIn(lines: List<String>, listPrefix: String) =
    lines.mapNotNull { itemContent(it, listPrefix) }.flatMap(::tagsOf).distinct().sorted()

/** Pure: every tag on an item, lower case, once each and sorted */
fun itemTags(lines: List<String>, listPrefix: String): List<String> = tagsIn(lines, listPrefix)

/** Pure: the tags on the items of the caret's section; null outside a section */
fun sectionTags(lines: List<String>, caretLine: Int, listPrefix: String): List<String>? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    return tagsIn(lines.subList(header + 1, sectionLastLine(lines, header) + 1), listPrefix)
}

private fun renamedLine(line: String, listPrefix: String, old: String, new: String): String {
    val content = itemContent(line, listPrefix) ?: return line
    return line.dropLast(content.length) + oneTag(old).replace(content, Regex.escapeReplacement("#${new.removePrefix("#")}"))
}

private fun renameBetween(lines: List<String>, range: IntRange, listPrefix: String, old: String, new: String): ListEdit {
    val after = lines.mapIndexed { i, line -> if (i in range) renamedLine(line, listPrefix, old, new) else line }
    return ListEdit(after, lines.indices.count { lines[it] != after[it] })
}

/** Pure: [old] renamed to [new] on every item; prose and headings keep theirs */
fun renameTagInLines(lines: List<String>, listPrefix: String, old: String, new: String): ListEdit =
    renameBetween(lines, lines.indices, listPrefix, old, new)

/** Pure: [old] renamed to [new] on the items of the caret's section; null outside a section */
fun renameTagInSection(lines: List<String>, caretLine: Int, listPrefix: String, old: String, new: String): ListEdit? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    return renameBetween(lines, header + 1..sectionLastLine(lines, header), listPrefix, old, new)
}

/** Pure: the section's bullets changed to [newPrefix]; every offered prefix is read, so it can be changed back */
fun changeSectionPrefix(lines: List<String>, caretLine: Int, listPrefix: String, newPrefix: String): ListEdit? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val body   = header + 1..sectionLastLine(lines, header)
    val after  = lines.mapIndexed { i, line ->
        val bullet = if (i in body) (listOf(listPrefix) + ITEM_PREFIXES).firstNotNullOfOrNull { parseBullet(line, it) } else null
        if (bullet == null) line else "${bullet.chevrons} $newPrefix ${bullet.content}"
    }
    return ListEdit(after, lines.indices.count { lines[it] != after[it] })
}

/** Pure: the section's items clustered under `// #tag` lines, numbered lists renumbered; null with fewer than two tags */
fun groupSectionByTag(lines: List<String>, caretLine: Int, listPrefix: String): List<String>? {
    var enough = false
    val result = applyToSection(lines, caretLine) { body ->
        val kept     = body.filterNot(GROUP_HEADING::matches)
        val firstTag = kept.associateWith { line -> itemContent(line, listPrefix)?.let { tagsOf(splitItem(it).body).firstOrNull() } }
        val groups   = kept.filter { firstTag[it] != null }.groupBy { firstTag[it]!! }
        enough = groups.size > 1
        if (!enough) body
        else renumberLines(groups.flatMap { (tag, items) -> listOf("// #$tag") + items } + kept.filter { firstTag[it] == null })
    }
    return result.takeIf { enough }
}

/** Pure: the `==N` word goal on [header], or null */
fun sectionGoal(header: String): Int? = WORD_GOAL.find(header)?.groupValues?.get(1)?.toIntOrNull()

/** Pure: [header] with its word goal set to [goal]; null removes it */
fun withSectionGoal(header: String, goal: Int?): String {
    val name = WORD_GOAL.replace(header, "").trimEnd()
    return if (goal == null) name else "$name ==$goal"
}
