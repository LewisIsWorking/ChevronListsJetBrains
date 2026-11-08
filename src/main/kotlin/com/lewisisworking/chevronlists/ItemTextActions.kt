/**
 * ItemTextActions.kt
 * IntelliJ bridge for ItemTextTransforms.kt. Each action applies to the caret
 * line, or to every item in the selection.
 */
package com.lewisisworking.chevronlists

abstract class ChevronItemTextAction(
    text: String,
    description: String,
    private val change: (String) -> String
) : ChevronLineTransformAction(text, description) {
    override fun transform(line: String, listPrefix: String): String? =
        transformItemLine(line, listPrefix, change)
}

class UppercaseItemAction : ChevronItemTextAction(
    "CL: Uppercase Item", "Change this item's words to UPPERCASE; markers, tags and links are left alone",
    { content -> changeItemWords(content) { it.uppercase() } }
)

class LowercaseItemAction : ChevronItemTextAction(
    "CL: Lowercase Item", "Change this item's words to lowercase; markers, tags and links are left alone",
    { content -> changeItemWords(content) { it.lowercase() } }
)

class TitleCaseItemAction : ChevronItemTextAction(
    "CL: Title Case Item", "Capitalise each of this item's words; markers, tags and links are left alone",
    { content -> changeItemWords(content, ::toTitleCase) }
)

class StrikethroughItemAction : ChevronItemTextAction(
    "CL: Strikethrough Item", "Toggle ~~strikethrough~~ on this item's words", ::toggleStrikethrough
)

class RemoveStrikethroughAction : ChevronItemTextAction(
    "CL: Remove Strikethrough", "Remove ~~strikethrough~~ from this item's words", ::removeStrikethrough
)
