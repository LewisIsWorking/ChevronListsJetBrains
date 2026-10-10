/**
 * Sets.kt
 * Pure logic for sets: a markdown heading tagged `#set` makes the `> Sections`
 * under it the buckets of one group (Paid, Hasn't paid, Not going), so each
 * item belongs in exactly one of them. A set runs until the next heading of
 * the same or a higher level. No IntelliJ Platform imports.
 */
package com.lewisisworking.chevronlists

private val SET_TAG              = Regex("""(?:^|\s)#set(?=\s|$)""", RegexOption.IGNORE_CASE)
private val CHECKBOX_MARK        = Regex("""^\[[ xX]?]\s*""")
private val TRAILING_PUNCTUATION = Regex("""[.,;:!?]+$""")
private val SPACES               = Regex("""\s+""")

/** A `#set` heading and the header lines of the sections under it */
data class ChevronSet(val headingLine: Int, val sections: List<Int>)

/** Every `#set` heading in the document, with its sections */
fun findSets(lines: List<String>): List<ChevronSet> = lines.indices.mapNotNull { i ->
    val heading = parseSubheading(lines[i])
    if (heading == null || !SET_TAG.containsMatchIn(heading.content)) return@mapNotNull null
    val sections = mutableListOf<Int>()
    for (j in i + 1 until lines.size) {
        val next = parseSubheading(lines[j])
        if (next != null && next.level <= heading.level) break
        if (isHeader(lines[j])) sections += j
    }
    ChevronSet(i, sections)
}

/** What two items must share to be the same member: case, a checkbox and end punctuation do not count */
fun setMemberKey(content: String): String =
    content.replace(CHECKBOX_MARK, "").trim().replace(TRAILING_PUNCTUATION, "").trim()
        .replace(SPACES, " ").lowercase()

private fun itemOf(line: String, listPrefix: String) =
    parseNumbered(line)?.let { it.chevrons to it.content } ?: parseBullet(line, listPrefix)?.let { it.chevrons to it.content }

/** The top-level item lines of the section headed at [header]; nested notes are left out */
fun sectionMembers(lines: List<String>, header: Int, listPrefix: String): List<Int> {
    val out = mutableListOf<Int>()
    for (i in header + 1 until lines.size) {
        if (isHeader(lines[i]) || parseSubheading(lines[i]) != null) break
        if (itemOf(lines[i], listPrefix)?.first?.length == 2) out += i
    }
    return out
}

private fun chevronsOf(line: String) = line.takeWhile { it == '>' }.length

private fun sectionName(lines: List<String>, header: Int) = parseHeader(lines[header])?.content?.trim().orEmpty()

/** Flags every item that is in a set more than once, naming where else it is */
fun collectSetDuplicates(lines: List<String>, listPrefix: String): List<DiagnosticIssue> {
    val out = mutableListOf<DiagnosticIssue>()
    for (set in findSets(lines)) {
        val where = LinkedHashMap<String, MutableList<Pair<Int, Int>>>()
        for (header in set.sections) for (line in sectionMembers(lines, header, listPrefix)) {
            val key = setMemberKey(itemOf(lines[line], listPrefix)!!.second)
            if (key.isNotEmpty()) where.getOrPut(key) { mutableListOf() } += line to header
        }
        for (hits in where.values.filter { it.size > 1 }) for ((line, _) in hits) {
            val others = hits.filter { it.first != line }
                .joinToString(", ") { "\"${sectionName(lines, it.second)}\" (line ${it.first + 1})" }
            out += DiagnosticIssue(line, "In this set more than once: also in $others", IssueKind.SET_DUPLICATE)
        }
    }
    return out
}

/** Where an item in the section at [fromHeader] can move: the other sections of its set, or of the document */
fun moveTargets(lines: List<String>, fromHeader: Int): List<Int> {
    val set = findSets(lines).firstOrNull { fromHeader in it.sections }
    return (set?.sections ?: lines.indices.filter { isHeader(lines[it]) }) - fromHeader
}

/** A target section's header line and name, as the Move to List popup shows it */
data class MoveTarget(val header: Int, val name: String)

/** The sections the item at [itemLine] can move to, or empty when the line is not a top-level item */
fun moveTargetsFor(lines: List<String>, itemLine: Int, listPrefix: String): List<MoveTarget> {
    if (itemOf(lines.getOrElse(itemLine) { "" }, listPrefix)?.first?.length != 2) return emptyList()
    val from = (itemLine downTo 0).firstOrNull { isHeader(lines[it]) } ?: return emptyList()
    return moveTargets(lines, from).map { MoveTarget(it, sectionName(lines, it)) }
}

private fun renumberSection(lines: MutableList<String>, header: Int) {
    var end = header + 1
    while (end < lines.size && !isHeader(lines[end]) && parseSubheading(lines[end]) == null) end++
    val body = lines.subList(header + 1, end)
    val renumbered = renumberLines(body.toList())
    for (i in renumbered.indices) body[i] = renumbered[i]
}

/** [line] in the list type of the target's existing top-level items: numbered or bulleted */
private fun matchListType(line: String, lines: List<String>, targetHeader: Int, listPrefix: String): String {
    val sibling = sectionMembers(lines, targetHeader, listPrefix).firstOrNull() ?: return line
    val (chevrons, content) = itemOf(line, listPrefix) ?: return line
    return if (parseNumbered(lines[sibling]) != null) "$chevrons 1. $content" else "$chevrons $listPrefix $content"
}

/**
 * The document with the top-level item at [itemLine], and anything nested under
 * it, moved to the end of the section headed at [targetHeader]. It takes the
 * target's list type, and both sections are renumbered. Null when the line is
 * not a top-level item or the target is its own section.
 */
fun computeMoveToSection(lines: List<String>, itemLine: Int, targetHeader: Int, listPrefix: String): List<String>? {
    if (moveTargetsFor(lines, itemLine, listPrefix).none { it.header == targetHeader }) return null
    val fromHeader = (itemLine downTo 0).first { isHeader(lines[it]) }
    var end = itemLine
    while (end + 1 < lines.size && chevronsOf(lines[end + 1]) > 2) end++
    val block = lines.subList(itemLine, end + 1).toMutableList()
    val rest = lines.toMutableList().apply { subList(itemLine, end + 1).clear() }
    val target = if (targetHeader > end) targetHeader - block.size else targetHeader

    // Land after the target's last item, before the blank lines that close it
    var insertAt = target + 1
    for (i in target + 1 until rest.size) {
        if (isHeader(rest[i]) || parseSubheading(rest[i]) != null) break
        if (chevronsOf(rest[i]) >= 2) insertAt = i + 1
    }
    block[0] = matchListType(block[0], rest, target, listPrefix)
    rest.addAll(insertAt, block)
    renumberSection(rest, target)
    renumberSection(rest, if (insertAt <= fromHeader) fromHeader + block.size else fromHeader)
    return rest
}

/**
 * The totals shown after the `#set` heading at [headingLine], such as
 * "23 Paid, 3 Hasn't paid, 3 Not going, 28 in all". "In all" counts each
 * member once, so a name in two lists is not counted twice. Null when the
 * line is not a set heading or the set has no sections.
 */
fun setSummary(lines: List<String>, headingLine: Int, listPrefix: String): String? {
    val set = findSets(lines).firstOrNull { it.headingLine == headingLine }
    if (set == null || set.sections.isEmpty()) return null
    val members = set.sections.associateWith { sectionMembers(lines, it, listPrefix) }
    val unique  = members.values.flatten().map { setMemberKey(itemOf(lines[it], listPrefix)!!.second) }.toSet().size
    return members.entries.joinToString(", ") { (header, items) -> "${items.size} ${sectionName(lines, header).trimEnd('.')}" } +
        ", $unique in all"
}
