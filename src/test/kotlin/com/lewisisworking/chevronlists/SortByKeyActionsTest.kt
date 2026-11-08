/**
 * SortByKeyActionsTest.kt
 * Proves the keyed sort actions are registered and edit the document.
 */
package com.lewisisworking.chevronlists

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SortByKeyActionsTest : BasePlatformTestCase() {

    private fun check(action: String, before: String, after: String) {
        myFixture.configureByText("notes.md", before)
        myFixture.performEditorAction(action)
        assertEquals(after, myFixture.editor.document.text)
    }

    fun `test sort by priority`() =
        check("ChevronLists.SortByPriority", "> A\n>> - <caret>a\n>> - !! b", "> A\n>> - !! b\n>> - a")

    fun `test sort by votes`() =
        check("ChevronLists.SortByVotes", "> A\n>> - <caret>a +1\n>> - b +2", "> A\n>> - b +2\n>> - a +1")

    fun `test sort by due date`() =
        check("ChevronLists.SortByDueDate", "> A\n>> - <caret>b\n>> - a @2026-01-01", "> A\n>> - a @2026-01-01\n>> - b")
}
