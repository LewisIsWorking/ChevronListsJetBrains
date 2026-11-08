/**
 * SectionMarkerActions.kt
 * IntelliJ bridge for SectionMarkers.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class MarkAllItemsDoneAction : ChevronSectionListAction(
    "CL: Mark All Items Done", "Tick every item in the section, adding a checkbox where there is none",
    { lines, prefix -> mapSectionItems(lines, prefix, ::markItemDone) }
)

class MarkAllItemsUndoneAction : ChevronSectionListAction(
    "CL: Mark All Items Undone", "Reset every done item in the section",
    { lines, prefix -> mapSectionItems(lines, prefix, ::markItemUndone) }
)

class RemoveAllCheckboxesAction : ChevronSectionListAction(
    "CL: Remove All Checkboxes", "Remove the checkbox from every item in the section",
    { lines, prefix -> mapSectionItems(lines, prefix, ::removeItemCheckbox) }
)

class ClearAllDueDatesAction : ChevronSectionListAction(
    "CL: Clear All Due Dates", "Remove the @YYYY-MM-DD due date from every item in the section",
    { lines, prefix -> mapSectionItems(lines, prefix, ::clearItemDueDates) }
)

class ClearAllPriorityAction : ChevronSectionListAction(
    "CL: Clear All Priority", "Remove the priority marker from every item in the section",
    { lines, prefix -> mapSectionItems(lines, prefix, ::clearItemPriority) }
)

class StripCommentsFromSectionAction : ChevronSectionListAction(
    "CL: Strip Comments from Section", "Remove the // comment from every item in the section",
    { lines, prefix -> mapSectionItems(lines, prefix, ::stripItemComment) }
)

class StripAllMetadataAction : ChevronSectionListAction(
    "CL: Strip All Metadata", "Remove tags, dates, priority, votes and other markers from every item in the section",
    { lines, prefix -> mapSectionItems(lines, prefix, ::stripItemMetadata) }
) {
    override fun actionPerformed(e: AnActionEvent) {
        val answer = Messages.showOkCancelDialog(
            e.project, "This will strip all tags, dates, priority, votes and other markers from every item in the section. Continue?",
            "Strip All Metadata", "Strip Metadata", "Cancel", null
        )
        if (answer == Messages.OK) super.actionPerformed(e)
    }
}

class AddVoteAction : ChevronLineTransformAction("CL: Add Vote", "Add one vote to the item at the caret") {
    override fun transform(line: String, listPrefix: String) =
        transformItemLine(line, listPrefix) { changeItemVotes(it, 1) }
}

class RemoveVoteAction : ChevronLineTransformAction("CL: Remove Vote", "Remove one vote from the item at the caret") {
    override fun transform(line: String, listPrefix: String) =
        transformItemLine(line, listPrefix) { changeItemVotes(it, -1) }
}
