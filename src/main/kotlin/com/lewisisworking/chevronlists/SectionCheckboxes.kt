/**
 * SectionCheckboxes.kt
 * Pure logic for Mark All Done / Mark All Undone over one section body. No
 * IntelliJ Platform imports. Only items that already have a checkbox change;
 * an item without one is left alone, as in the VS Code extension.
 */
package com.lewisisworking.chevronlists

private val CHECKBOX = Regex("""^\[( |x|X)?]\s*""")

/** Pure: [content] with its leading checkbox set to done or undone, or null when it has none */
fun setCheckbox(content: String, done: Boolean): String? {
    val trimmed = content.trimStart()
    val match = CHECKBOX.find(trimmed) ?: return null
    return (if (done) "[x] " else "[ ] ") + trimmed.substring(match.range.last + 1)
}

/** Pure: every checkbox item in [lines] marked done (or undone); other lines unchanged */
fun markAll(lines: List<String>, done: Boolean, listPrefix: String): List<String> =
    lines.map { text ->
        parseNumbered(text)?.let { n ->
            setCheckbox(n.content, done)?.let { return@map "${n.chevrons} ${n.num}. $it" }
        }
        parseBullet(text, listPrefix)?.let { b ->
            setCheckbox(b.content, done)?.let { return@map "${b.chevrons} ${b.prefix} $it" }
        }
        text
    }
