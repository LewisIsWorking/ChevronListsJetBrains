/**
 * ItemReportActionsTest.kt
 * Runs the item report actions in a headless IDE. Actions that open a list are
 * run on files with nothing to list, as a popup cannot show here.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ItemReportActionsTest : BasePlatformTestCase() {

    private val ids = listOf(
        "ShowUpcomingDueDates", "ShowRecurringItems", "ShowOldItems", "ShowTimeEstimates", "ShowSectionSummary",
        "CountItemsByTag", "ShowTagStats", "FilterByMention", "ShowMentionsReport",
    )

    fun `test every report is registered`() {
        for (id in ids) assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
    }

    fun `test reports with nothing to list leave the text alone`() {
        for (id in ids) {
            myFixture.configureByText("notes.md", "<caret>> A\n>> - a")
            myFixture.performEditorAction("ChevronLists.$id")
            assertEquals("> A\n>> - a", myFixture.editor.document.text)
        }
    }
}
