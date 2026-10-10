/**
 * Templates.kt
 * Pure logic for section templates, ported from the VS Code extension's
 * templateData.ts and saveTemplate.ts. Bodies use VS Code snippet syntax
 * (`${1:default}`, `$1`, `$0`) so templates move between the two editors
 * unchanged; liveTemplateText turns one into IntelliJ live template text.
 */
package com.lewisisworking.chevronlists

/** A built-in or saved template; [body] is VS Code snippet syntax */
data class ChevronTemplate(val name: String, val description: String, val body: String)

/** The built-in templates, the same five as the VS Code extension */
val BUILT_IN_TEMPLATES = listOf(
    ChevronTemplate("Bullet List", "Standard chevron bullet list",
        "> \${1:Section Header}\n>> - \${2:First item}\n>> - \${3:Second item}\n>> - \$0"),
    ChevronTemplate("Numbered List", "Chevron numbered list",
        "> \${1:Section Header}\n>> 1. \${2:First item}\n>> 2. \${3:Second item}\n>> 3. \$0"),
    ChevronTemplate("Nested List", "Two-level nested bullet list",
        "> \${1:Section Header}\n>> - \${2:Top item}\n>>> - \${3:Nested item}\n>>> - \${4:Nested item}\n>> - \$0"),
    ChevronTemplate("Session Notes", "RPG / meeting session notes template",
        "> \${1:Session Title}\n>> - \${2:Key event}\n>> - \${3:Key event}\n>> - \${4:Key event}\n>> - Notes: \$0"),
    ChevronTemplate("Character Sheet", "Quick character / entity profile",
        "> \${1:Character Name}\n>> - Role: \${2:role}\n>> - Traits: \${3:traits}\n>> - Notes: \$0"),
)

/** One piece of a snippet body: literal text, a numbered tab stop, or the final caret ($0) */
sealed interface SnippetPart {
    data class Text(val text: String) : SnippetPart
    data class Stop(val index: Int, val default: String) : SnippetPart
    data object End : SnippetPart
}

private val STOP = Regex("""\$\{(\d+):((?:\\.|[^\\}])*)}|\$(\d+)|\\(.)""")

/** Splits a snippet body into its parts; `\$`, `\}` and `\\` are literal characters */
fun parseSnippet(body: String): List<SnippetPart> {
    val parts = mutableListOf<SnippetPart>()
    val text  = StringBuilder()
    fun flush() { if (text.isNotEmpty()) { parts += SnippetPart.Text(text.toString()); text.clear() } }
    var at = 0
    for (m in STOP.findAll(body)) {
        text.append(body, at, m.range.first)
        at = m.range.last + 1
        val (braced, default, bare, escaped) = m.destructured
        when {
            escaped.isNotEmpty() -> text.append(escaped)
            else -> {
                flush()
                val index = (braced.ifEmpty { bare }).toInt()
                parts += if (index == 0) SnippetPart.End
                         else SnippetPart.Stop(index, default.replace(Regex("""\\(.)"""), "$1"))
            }
        }
    }
    text.append(body, at, body.length)
    flush()
    return parts
}

/** A live template's text and its variables (name to default) in tab order */
data class LiveTemplateText(val text: String, val variables: List<Pair<String, String>>)

/**
 * IntelliJ live template text for [parts]: stops become `$V1$` variables
 * (tab order follows the stop numbers, as in VS Code; a repeated number
 * mirrors), the final caret becomes `$END$`, and literal `$` is doubled.
 */
fun liveTemplateText(parts: List<SnippetPart>): LiveTemplateText {
    val text = StringBuilder()
    val defaults = sortedMapOf<Int, String>()
    for (part in parts) when (part) {
        is SnippetPart.Text -> text.append(part.text.replace("$", "$$"))
        is SnippetPart.Stop -> { text.append("\$V${part.index}\$"); defaults.putIfAbsent(part.index, part.default) }
        SnippetPart.End     -> text.append("\$END\$")
    }
    return LiveTemplateText(text.toString(), defaults.map { (i, d) -> "V$i" to d })
}

/** Escapes text for use as a snippet placeholder default */
private fun escapeDefault(text: String) = text.replace(Regex("""[\\$}]"""), """\\$0""")

/** The `> Header` line of the section holding [caretLine], or null when there is none */
fun sectionHeaderLine(lines: List<String>, caretLine: Int): Int? {
    val header = (sectionBody(lines, caretLine) ?: return null).first - 1
    return header.takeIf { it >= 0 && isHeader(lines[it]) }
}

/**
 * The section at [headerLine] as a snippet body: the header name and each
 * item's text become tab stops, other lines are dropped, and the caret ends
 * on a new line below. Item text containing `$`, `}` or `\` is escaped.
 */
fun sectionToSnippetBody(lines: List<String>, headerLine: Int, prefix: String): String {
    val out  = mutableListOf("> \${1:${escapeDefault(parseHeader(lines[headerLine])?.content.orEmpty())}}")
    var stop = 2
    // From a header line, sectionBody is everything after it up to the next header
    for (i in sectionBody(lines, headerLine) ?: IntRange.EMPTY) {
        val bullet   = parseBullet(lines[i], prefix)
        val numbered = parseNumbered(lines[i])
        if (bullet != null)
            out += "${bullet.chevrons} $prefix \${${stop++}:${escapeDefault(bullet.content.ifEmpty { "item" })}}"
        else if (numbered != null)
            out += "${numbered.chevrons} ${numbered.num}. \${${stop++}:${escapeDefault(numbered.content.ifEmpty { "item" })}}"
    }
    out += "\$0"
    return out.joinToString("\n")
}
