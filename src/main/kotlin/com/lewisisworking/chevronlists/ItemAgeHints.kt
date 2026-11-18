/**
 * ItemAgeHints.kt
 * Pure logic for the item-age inlay hints: an end-of-line note on every item
 * whose `@created:` stamp is 30 or more days old, showing its age in days.
 * No IntelliJ Platform imports - fully testable with plain JUnit.
 *
 * The JB-native counterpart of the VS Code extension's age highlight
 * (CL: Toggle Age Highlight): VS Code mutes old items with a faded italic
 * decoration, while IntelliJ draws the age as an inlay hint, which survives
 * soft wrap. Toggled in Settings, Editor, Inlay Hints, like every provider.
 */
package com.lewisisworking.chevronlists

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Pure: the age hint for the item with [content], or null when it has no `@created:` stamp or is under 30 days old. */
fun itemAgeHint(content: String, today: LocalDate): String? {
    val created = createdOn(splitItem(content)) ?: return null
    val age = ChronoUnit.DAYS.between(created, today)
    if (age < 30) return null
    return "(${age}d old)"
}
