/**
 * FileExports.kt
 * Pure logic for exporting a whole file as JSON, CSV, HTML or a plain markdown
 * document. No IntelliJ Platform imports - fully testable with plain JUnit.
 * Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

private val LINK_OR_TAG = Regex("""\[\[([^\]]+)]]|(?<!\S)#(\w[\w-]*)""")
private val NOT_SLUG    = Regex("""[^a-z0-9]+""")

private class FileItem(val line: Int, val depth: Int, val num: Int?, val content: String)

private class FileSection(val line: Int, val name: String, val items: List<FileItem>)

private class StructuredItem(val text: String, val tags: List<String>, val priority: Int?, val dueDate: String?, val done: Boolean?)

private fun fileItem(lines: List<String>, line: Int, listPrefix: String): FileItem? {
    val content  = itemContent(lines[line], listPrefix) ?: return null
    val numbered = parseNumbered(lines[line])
    val chevrons = numbered?.chevrons ?: parseBullet(lines[line], listPrefix)!!.chevrons
    return FileItem(line, chevrons.length - 2, numbered?.num, content)
}

private fun fileSections(lines: List<String>, listPrefix: String): List<FileSection> =
    lines.indices.filter { isHeader(lines[it]) }.map { header ->
        val items = (header + 1..sectionLastLine(lines, header)).mapNotNull { fileItem(lines, it, listPrefix) }
        FileSection(header, parseHeader(lines[header])!!.content.trim(), items)
    }

private fun isDone(parts: ItemParts) = parts.check.equals("[x]", ignoreCase = true)

/** The item's words with its checkbox, priority and first due date taken out into their own fields */
private fun structured(item: FileItem): StructuredItem {
    val parts = splitItem(item.content)
    val date  = ITEM_DUE_DATE.find(parts.body)
    val body  = date?.let { parts.body.removeRange(it.range) } ?: parts.body
    return StructuredItem(
        text     = joinItem(parts.copy(check = "", priority = "", body = body)),
        tags     = tagFields(item.content),
        priority = parts.priority.length.takeIf { it > 0 },
        dueDate  = date?.groupValues?.get(1),
        done     = if (parts.check.isEmpty()) null else isDone(parts),
    )
}

/** Pure: every section and its items as JSON, each item's fields separated out */
fun fileAsJson(lines: List<String>, listPrefix: String): String =
    toJson(fileSections(lines, listPrefix).map { section ->
        linkedMapOf(
            "line"  to section.line,
            "name"  to section.name,
            "items" to section.items.map { item ->
                val fields = structured(item)
                linkedMapOf(
                    "line"     to item.line,
                    "depth"    to item.depth,
                    "numbered" to (item.num != null),
                    "num"      to item.num,
                    "content"  to fields.text,
                    "tags"     to fields.tags,
                    "priority" to fields.priority,
                    "dueDate"  to fields.dueDate,
                    "done"     to fields.done,
                )
            },
        )
    })

/** Pure: one CSV row per item, naming its section */
fun fileAsCsv(lines: List<String>, listPrefix: String): String {
    val rows = fileSections(lines, listPrefix).flatMap { section ->
        section.items.map { item ->
            val fields = structured(item)
            listOf(
                csvEscape(section.name), item.depth, item.num != null, item.num ?: "", csvEscape(fields.text),
                csvEscape(fields.tags.joinToString(" ")), fields.priority ?: "", fields.dueDate ?: "", fields.done ?: "",
            ).joinToString(",")
        }
    }
    return (listOf("Section,Depth,Numbered,Number,Content,Tags,Priority,DueDate,Done") + rows).joinToString("\n")
}

private fun slugOf(name: String) = name.lowercase().replace(NOT_SLUG, "-").trim('-')

/** The item's words as HTML, its [[links]] as anchors and its #tags as badges */
private fun renderedItem(text: String): String = buildString {
    var at = 0
    for (match in LINK_OR_TAG.findAll(text)) {
        append(escapeHtml(text.substring(at, match.range.first)))
        val link = match.groups[1]?.value
        if (link != null) append("""<a class="cl-link" href="#section-${slugOf(link)}">${escapeHtml(link)}</a>""")
        else append("""<span class="cl-tag">#${escapeHtml(match.groupValues[2])}</span>""")
        at = match.range.last + 1
    }
    append(escapeHtml(text.substring(at)))
}

private fun htmlItem(item: FileItem): String {
    val parts  = splitItem(item.content)
    val done   = if (isDone(parts)) " cl-done" else ""
    val indent = if (item.depth > 0) """ style="margin-left:${item.depth * 1.25}rem"""" else ""
    val check  = when {
        parts.check.isEmpty() -> ""
        isDone(parts)         -> """<span class="cl-check">✅</span> """
        else                  -> """<span class="cl-check">☐</span> """
    }
    val num = item.num?.let { """<span class="cl-num">$it.</span> """ } ?: ""
    return """<li class="cl-item$done"$indent>$check$num${renderedItem(joinItem(parts.copy(check = "")))}</li>"""
}

private val HTML_STYLE = """
<style>
  :root { --accent: #E5C07B; --muted: #5C6370; --bg: #1e2127; --surface: #282c34; --text: #abb2bf; --done: #5C6370; }
  body  { font-family: system-ui, sans-serif; background: var(--bg); color: var(--text); max-width: 860px; margin: 2rem auto; padding: 0 1.5rem; }
  h1    { color: var(--accent); border-bottom: 1px solid var(--muted); padding-bottom: .5rem; }
  details { background: var(--surface); border-radius: 6px; margin: .75rem 0; overflow: hidden; }
  summary.cl-header { cursor: pointer; padding: .65rem 1rem; font-weight: bold; color: var(--accent); list-style: none; }
  summary.cl-header::before { content: "▶ "; font-size: .75em; opacity: .6; }
  details[open] summary.cl-header::before { content: "▼ "; }
  ul.cl-list  { margin: 0; padding: .5rem 1rem 1rem 1.5rem; list-style: none; }
  li.cl-item  { padding: .2rem 0; font-size: .93rem; }
  li.cl-done  { color: var(--done); text-decoration: line-through; }
  .cl-check   { font-size: .9em; }
  .cl-num     { color: #61AFEF; font-size: .85em; margin-right: .25rem; }
  .cl-tag     { background: #3e4451; color: #98C379; border-radius: 3px; padding: 1px 5px; font-size: .8em; margin: 0 1px; }
  .cl-link    { color: #61AFEF; text-decoration: none; border-bottom: 1px dotted #61AFEF; }
  .cl-link:hover { border-bottom-style: solid; }
</style>
""".trim()

/** Pure: a standalone HTML page with a collapsible block per section. Items above the first section are left out */
fun fileAsHtml(lines: List<String>, listPrefix: String, fileName: String): String {
    val title = escapeHtml(fileName)
    val body  = fileSections(lines, listPrefix).joinToString("\n") { section ->
        listOf(
            """<details id="section-${slugOf(section.name)}" open>""",
            """  <summary class="cl-header">${escapeHtml(section.name)}</summary>""",
            """  <ul class="cl-list">${section.items.joinToString("\n", transform = ::htmlItem)}</ul>""",
            "</details>",
        ).joinToString("\n")
    }
    return listOf(
        "<!DOCTYPE html>", """<html lang="en">""", "<head>", """<meta charset="UTF-8">""",
        """<meta name="viewport" content="width=device-width,initial-scale=1">""",
        "<title>$title</title>", HTML_STYLE, "</head>", "<body>", "<h1>$title</h1>", body, "</body>", "</html>",
    ).joinToString("\n")
}

/** Pure: the file as standard markdown, sections as ## headings and items as nested lists */
fun fileAsMarkdown(lines: List<String>, listPrefix: String): String {
    val out = mutableListOf<String>()
    for (line in lines) {
        if (isHeader(line)) {
            if (out.isNotEmpty()) out += ""
            out += "## ${parseHeader(line)!!.content.trim()}"
            out += ""
            continue
        }
        val bullet   = parseBullet(line, listPrefix)
        val numbered = parseNumbered(line)
        when {
            bullet != null   -> out += "${"  ".repeat(bullet.chevrons.length - 2)}- ${bullet.content}"
            numbered != null -> out += "${"  ".repeat(numbered.chevrons.length - 2)}${numbered.num}. ${numbered.content}"
        }
    }
    return out.joinToString("\n").trimEnd() + "\n"
}

/** Pure: every section with each item's full set of fields as JSON, under the file's name */
fun allSectionsAsJson(lines: List<String>, listPrefix: String, fileName: String): String =
    toJson(linkedMapOf(
        "file"     to fileName,
        "sections" to fileSections(lines, listPrefix).map { section ->
            linkedMapOf(
                "section" to section.name,
                "items"   to section.items.map { itemFields(it.depth, it.num, it.content) },
            )
        },
    ))
