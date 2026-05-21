/**
 * TagCompletion.kt
 * Pure logic for collecting all unique tags in a document, used by the
 * completion contributor to suggest existing tags. No IntelliJ Platform
 * imports - fully testable.
 */
package com.lewisisworking.chevronlists

/**
 * Pure: scans the document text and returns every distinct #tag occurrence
 * across all lines, as a set of strings each starting with `#`.
 *
 * Re-uses `findTags` from InlinePatterns.kt so the detection rules stay
 * consistent with the highlighting layer.
 */
fun extractAllTags(text: String): Set<String> {
    val tags = mutableSetOf<String>()
    for (line in text.split("\n")) {
        for (range in findTags(line)) {
            tags += line.substring(range.first, range.last + 1)
        }
    }
    return tags
}