/**
 * Tips.kt
 * Pure data for the tip of the day: a rotating hint shown on project open
 * until dismissed. Adapted from the VS Code extension's tips.ts: bodies
 * describe the IntelliJ plugin (right-click menu, Find Action), and each tip
 * names the action its Try It button runs, or null when there is none.
 * No IntelliJ Platform imports - fully testable with plain JUnit.
 */
package com.lewisisworking.chevronlists

/** One rotating tip: the action Try It runs, or null for text-only tips */
data class ChevronTip(
    val title: String,
    val body: String,
    val actionId: String? = null,
    val keybinding: String? = null
)

/** The notification text for [tip], with its keybinding hint when it has one */
fun tipMessage(tip: ChevronTip): String =
    "💡 ${tip.title} - ${tip.body}" + (tip.keybinding?.let { "  [$it]" } ?: "")

/** The tip after [index], wrapping around */
fun nextTip(tips: List<ChevronTip>, index: Int): Pair<Int, ChevronTip> {
    val next = (index + 1) % tips.size
    return next to tips[next]
}

val TIPS: List<ChevronTip> = listOf(
    ChevronTip(
        "Smart Enter",
        "Press Enter after a section header to auto-start a `>> -` item. Press Enter on an empty item to stop the list."
    ),
    ChevronTip(
        "Promote and demote",
        "CL: Promote Item moves an item one depth deeper - child items move with the parent. CL: Demote Item moves it back."
    ),
    ChevronTip(
        "Jump between sections",
        "Jump to the next or previous section header instantly.",
        keybinding = "Ctrl+Alt+Down / Ctrl+Alt+Up"
    ),
    ChevronTip(
        "Toggle note",
        "Add or remove an inline `>> > Note` line on the item at the cursor.",
        actionId = "ChevronLists.ToggleNote"
    ),
    ChevronTip(
        "Priority colours",
        "Add `!`, `!!`, or `!!!` at the start of any item for yellow, amber, or red background highlighting."
    ),
    ChevronTip(
        "Due dates",
        "Add `@YYYY-MM-DD` to an item to set a due date. CL: Show Upcoming Due Dates lists what is coming up.",
        actionId = "ChevronLists.ShowUpcomingDueDates"
    ),
    ChevronTip(
        "Tag autocomplete",
        "Type `#` in any item to get tag suggestions from the current file, sorted by how often you use them."
    ),
    ChevronTip(
        "Section links",
        "Link sections with `[[name]]` and jump to them with CL: Go to Linked Section."
    ),
    ChevronTip(
        "Focus Timer",
        "Start a configurable Pomodoro-style countdown (default 25 min) in the status bar.",
        actionId = "ChevronLists.StartFocusTimer"
    ),
    ChevronTip(
        "Quick Stats",
        "Get an instant one-line summary of the section: items, done %, words, tags, overdue.",
        actionId = "ChevronLists.QuickStats"
    ),
    ChevronTip(
        "Kanban view",
        "See all items grouped as Todo, In Progress (starred), and Done.",
        actionId = "ChevronLists.ShowKanban"
    ),
    ChevronTip(
        "Smart Paste",
        "CL: Smart Paste detects whether your clipboard is a numbered list, bullet list, or plain text and converts it automatically.",
        actionId = "ChevronLists.SmartPaste"
    ),
    ChevronTip(
        "Send to Daily Note",
        "Copy the item at cursor to today's daily note under `> Inbox`. Configure the folder in settings.",
        actionId = "ChevronLists.SendToDailyNote"
    ),
    ChevronTip(
        "Sort by Due Date",
        "Reorder items in the current section ascending by `@date` - undated items go last.",
        actionId = "ChevronLists.SortByDueDate"
    ),
    ChevronTip(
        "Replace in Section",
        "Find and replace text across the words of every item in a section, with a preview count before applying.",
        actionId = "ChevronLists.ReplaceInSection"
    ),
    ChevronTip(
        "Tag Stats",
        "Every `#tag` in the file with its item count, done count, and a completion bar.",
        actionId = "ChevronLists.ShowTagStats"
    ),
    ChevronTip(
        "Section Growth",
        "A bar chart of item count per section - great for seeing where work is concentrated.",
        actionId = "ChevronLists.ShowSectionGrowth"
    ),
    ChevronTip(
        "Colour presets",
        "Choose a co-ordinated colour palette in Settings, Tools, Chevron Lists, Appearance."
    ),
    ChevronTip(
        "Multi-cursor",
        "CL: Toggle Done (All Cursors) and CL: Set Priority (All Cursors) act on every active cursor simultaneously.",
        actionId = "ChevronLists.ToggleDoneAllCursors"
    ),
    ChevronTip(
        "Inline calculation",
        "Add `=2+2` anywhere in an item, then run CL: Evaluate Expression in Item to replace it with the result.",
        actionId = "ChevronLists.EvaluateExpression"
    ),
    ChevronTip(
        "Time estimates",
        "Add `~2h` or `~30m` to items, then CL: Show Section Time Estimate sums them all - great for sprint planning.",
        actionId = "ChevronLists.ShowSectionTimeEstimate"
    ),
    ChevronTip(
        "Clone Section",
        "CL: Clone Section duplicates the entire section immediately below with a `(copy)` suffix - perfect for reusable templates.",
        actionId = "ChevronLists.CloneSection"
    ),
    ChevronTip(
        "Word Count Goals",
        "Add `==500` to a section header to set a word goal. CL: Show Word Count Goals shows progress across every goal section.",
        actionId = "ChevronLists.ShowWordCountGoals"
    ),
    ChevronTip(
        "Rich text",
        "Use CL: Bold Text, CL: Italic Text, CL: Underline Text, or CL: Text Transform to wrap selected text - applies again to unwrap.",
        actionId = "ChevronLists.TextTransform"
    )
)
