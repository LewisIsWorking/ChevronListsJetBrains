/**
 * Statistics.kt
 * Pure logic for Quick Stats and Show File Statistics: per-section counts of
 * items, words, done checkboxes, tags, colour labels and overdue dates, and
 * totals for the file. No IntelliJ Platform imports. Mirrors statistics.ts and
 * quickStatsCommands.ts in the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** Counts for one `> Section`: items are its list lines at any depth */
data class SectionStats(
    val name: String,
    val line: Int,
    val items: Int,
    val words: Int,
    val done: Int,
    val checks: Int,
    val tags: Int,
    val coloured: Int,
    val overdue: Int,
)

/** Counts for a whole file, with the sections they came from */
data class FileStats(val sections: List<SectionStats>) {
    val totalItems: Int    get() = sections.sumOf { it.items }
    val totalWords: Int    get() = sections.sumOf { it.words }
    val totalDone: Int     get() = sections.sumOf { it.done }
    val totalChecks: Int   get() = sections.sumOf { it.checks }
    val totalTags: Int     get() = sections.sumOf { it.tags }
    val totalColoured: Int get() = sections.sumOf { it.coloured }
    val totalOverdue: Int  get() = sections.sumOf { it.overdue }
    val avgItems: Double   get() = if (sections.isEmpty()) 0.0 else totalItems.toDouble() / sections.size
    val mostPopulated: SectionStats?  get() = sections.maxByOrNull { it.items }
    val leastPopulated: SectionStats? get() = sections.minByOrNull { it.items }
}

private val CHECK_MARK   = Regex("""^\[(x|X| ?)]""")
private val LEADING      = listOf(Regex("""^\[(?:[xX]| ?)]\s*"""), Regex("""^!{1,3} +"""), Regex("""^\* +"""), Regex("""^\? +"""))
private val ANYWHERE     = listOf(
    Regex("""(?:^|\s+)//.*$"""),                          // inline comment
    Regex("""(?:^|\s+)\+\d+\s*$"""),                      // votes
    Regex("""(?:^|\s)#[A-Za-z][\w-]*"""),                 // tags
    Regex("""@created:\d{4}-\d{2}-\d{2}"""),              // creation dates
    Regex("""@\d{4}-\d{2}-\d{2}"""),                      // due dates
    Regex("""@(?:daily|weekly|monthly)"""),               // recurrence
    Regex("""~\d+h(?:\d+m)?"""), Regex("""~\d+m"""),      // time estimates
    Regex("""\{(?:red|green|blue|yellow|orange|purple)}\s*"""), // colour labels
    Regex("""\[\[[\w\s]+]]"""), Regex("""\[\[file:[^]]+]]"""),  // section and file links
)
private val STRIKE       = Regex("""~~(.+?)~~""")
private val DUE_DATE     = Regex("""(?:^|\s)@(\d{4}-\d{2}-\d{2})""")

/** Pure: the words the author wrote in an item, with checkboxes, tags, dates and other markers left out */
fun itemWordCount(content: String): Int {
    var s = content.trim()
    for (r in LEADING) s = r.replaceFirst(s, "")
    for (r in ANYWHERE) s = r.replace(s, "")
    s = STRIKE.replace(s, "$1").replace(Regex("""\s+"""), " ").trim()
    return if (s.isEmpty()) 0 else s.split(' ').size
}

private fun isSectionEnd(text: String) = parseHeader(text) != null || parseSubheading(text) != null

/** Pure: the counts for the section headed at [header]; [today] is an ISO date, for overdue */
fun sectionStats(lines: List<String>, header: Int, listPrefix: String, today: String): SectionStats {
    var items = 0; var words = 0; var done = 0; var checks = 0; var tags = 0; var coloured = 0; var overdue = 0
    var i = header + 1
    while (i < lines.size && !isSectionEnd(lines[i])) {
        val content = parseNumbered(lines[i])?.content ?: parseBullet(lines[i], listPrefix)?.content
        i++
        if (content.isNullOrBlank()) continue
        items++
        words += itemWordCount(content)
        tags  += findTags(content).size
        if (parseColourLabel(content) != null) coloured++
        CHECK_MARK.find(content.trim())?.let { checks++; if (it.groupValues[1].equals("x", ignoreCase = true)) done++ }
        if (DUE_DATE.findAll(content).any { it.groupValues[1] < today }) overdue++
    }
    return SectionStats(parseHeader(lines[header])!!.content.trim(), header, items, words, done, checks, tags, coloured, overdue)
}

/** Pure: the counts for every section in the file */
fun fileStats(lines: List<String>, listPrefix: String, today: String): FileStats =
    FileStats(lines.indices.filter { isHeader(lines[it]) }.map { sectionStats(lines, it, listPrefix, today) })

/** Pure: the header line of the section [line] is in, or -1 above the first one */
fun sectionHeaderAt(lines: List<String>, line: Int): Int {
    var i = line.coerceAtMost(lines.size - 1)
    while (i >= 0 && !isHeader(lines[i])) {
        if (parseSubheading(lines[i]) != null) return -1
        i--
    }
    return i
}

private fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

/** Pure: the one-line Quick Stats message, such as `"Paid" - 3 items, 1/3 done (33%), 4 words` */
fun quickStatsText(s: SectionStats): String {
    val pct   = if (s.items > 0) "${Math.round(s.done * 100.0 / s.items)}%" else "-"
    val parts = listOfNotNull(
        plural(s.items, "item"),
        "${s.done}/${s.items} done ($pct)",
        plural(s.words, "word"),
        if (s.tags > 0) plural(s.tags, "tag") else null,
        if (s.overdue > 0) "${s.overdue} overdue" else null,
    )
    return "\"${s.name}\" - ${parts.joinToString(", ")}"
}

/** Pure: the Show File Statistics report, totals first, then a line per section */
fun fileStatsReport(f: FileStats): String = buildString {
    appendLine("${plural(f.sections.size, "section")}, ${plural(f.totalItems, "item")}, ${plural(f.totalWords, "word")}")
    appendLine("Done: ${f.totalDone} of ${f.totalChecks} checkboxes")
    appendLine("Tags: ${f.totalTags}, colour labels: ${f.totalColoured}, overdue: ${f.totalOverdue}")
    appendLine("Average: ${"%.1f".format(java.util.Locale.ROOT, f.avgItems)} items per section")
    f.mostPopulated?.let { appendLine("Most items: \"${it.name}\" (${it.items})") }
    f.leastPopulated?.let { appendLine("Fewest items: \"${it.name}\" (${it.items})") }
    if (f.sections.isNotEmpty()) appendLine()
    for (s in f.sections) {
        val checks = if (s.checks > 0) ", ${s.done}/${s.checks} done" else ""
        appendLine("${s.name}: ${plural(s.items, "item")}, ${plural(s.words, "word")}$checks")
    }
}.trimEnd()
