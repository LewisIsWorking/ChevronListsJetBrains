/**
 * TemplateFiles.kt
 * Pure logic for moving templates in and out of markdown files, ported from
 * the VS Code extension's templateFileCommands.ts: every `> Header` section
 * of a file imports as a template, and export writes each template as the
 * section it inserts. A file exported from either editor imports into both.
 */
package com.lewisisworking.chevronlists

private val IMPORT_ITEM = Regex("""^(>{2,} \S+ )(.*)$""")

private fun escapeSnippet(text: String) = text.replace(Regex("""[\\$}]"""), """\\$0""")

/** Every section of [lines] as a template named after its header; items at every depth become fields */
fun importTemplates(lines: List<String>, fileName: String): List<ChevronTemplate> =
    lines.indices.filter { isHeader(lines[it]) }.map { header ->
        val name = parseHeader(lines[header])?.content.orEmpty()
        val body = mutableListOf("> \${1:${escapeSnippet(name)}}")
        var stop = 2
        for (i in sectionBody(lines, header) ?: IntRange.EMPTY) {
            val item = IMPORT_ITEM.matchEntire(lines[i]) ?: continue
            body += "${item.groupValues[1]}\${${stop++}:${escapeSnippet(item.groupValues[2].ifEmpty { "item" })}}"
        }
        body += "\$0"
        ChevronTemplate(name, "Imported from $fileName", body.joinToString("\n"))
    }

/** The text a snippet body inserts, with every field at its default */
fun snippetToText(body: String): String = parseSnippet(body).joinToString("") { part ->
    when (part) {
        is SnippetPart.Text -> part.text
        is SnippetPart.Stop -> part.default
        SnippetPart.End     -> ""
    }
}

/**
 * [templates] as one markdown file, a blank line between them. A template
 * whose text starts with its own `> Header` is written as it is; any other
 * gets its name as the header, so importing the file gives it back.
 */
fun exportTemplates(templates: List<ChevronTemplate>): String =
    templates.joinToString("\n\n") { t ->
        val text = snippetToText(t.body).trimEnd()
        if (isHeader(text.substringBefore("\n"))) text else "> ${t.name}\n$text"
    } + "\n"
