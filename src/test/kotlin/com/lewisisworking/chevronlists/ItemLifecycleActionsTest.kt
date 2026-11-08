/**
 * ItemLifecycleActionsTest.kt
 * Runs the age, recurrence, grouping and archive actions in a headless IDE. The
 * reports are run on files with nothing to list, as a popup cannot show here.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ItemLifecycleActionsTest : BasePlatformTestCase() {

    private val reports = listOf("ShowItemAgeReport", "ShowAgeStats", "ShowWordCountGoals", "ShowItemComplexity", "FilterByMultipleTags")
    private val edits   = listOf("GenerateNextOccurrence", "InsertRecurringItem", "GroupItemsByMention", "CollectItemsByTag", "RemoveOldItems", "ArchiveOldDoneItems")

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
            TestDialogManager.setTestDialog(TestDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun answer(text: String) = TestDialogManager.setTestInputDialog { text }

    private fun runAction(id: String, text: String): String {
        myFixture.configureByText("notes.md", text)
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    fun `test every action is registered`() {
        for (id in reports + edits) assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
    }

    fun `test reports with nothing to list leave the text alone`() {
        for (id in reports) assertEquals("> A\n>> - a", runAction(id, "<caret>> A\n>> - a"))
    }

    fun `test next occurrence is added below the item at the end of a file`() {
        assertEquals(">> - a @daily @2026-01-01\n>> - a @daily @2026-01-02", runAction("GenerateNextOccurrence", ">> - a<caret> @daily @2026-01-01"))
    }

    fun `test next occurrence leaves a plain item alone`() {
        assertEquals(">> - a", runAction("GenerateNextOccurrence", ">> - a<caret>"))
    }

    fun `test insert recurring item goes below the caret`() {
        answer("2")
        assertEquals("> A\n>> - [ ] Weekly review @weekly\n>> - b", runAction("InsertRecurringItem", "> A<caret>\n>> - b"))
        assertEquals(1, myFixture.editor.caretModel.logicalPosition.line)
    }

    fun `test group by mention rewrites the section`() {
        assertEquals("> S\n>> -- Sam\n>> - a @Sam\n>> -- Unassigned\n>> - b", runAction("GroupItemsByMention", "> S<caret>\n>> - b\n>> - a @Sam"))
    }

    fun `test collect by tag adds a results section`() {
        answer("#x")
        assertEquals("> S\n>> - a #x\n\n> #x Results\n>> - a #x\n", runAction("CollectItemsByTag", "> S<caret>\n>> - a #x"))
    }

    fun `test remove old items asks first`() {
        answer("30")
        TestDialogManager.setTestDialog(TestDialog.OK)
        assertEquals("> S", runAction("RemoveOldItems", "> S<caret>\n>> - a @created:2020-01-01"))
    }

    fun `test remove old items can be cancelled`() {
        answer("30")
        TestDialogManager.setTestDialog(TestDialog.NO)
        assertEquals("> S\n>> - a @created:2020-01-01", runAction("RemoveOldItems", "> S<caret>\n>> - a @created:2020-01-01"))
    }

    fun `test archive old done items moves them`() {
        answer("30")
        TestDialogManager.setTestDialog(TestDialog.OK)
        assertEquals(
            "> S\n>> - b\n\n> Archive\n>> - [x] a @created:2020-01-01",
            runAction("ArchiveOldDoneItems", "> S<caret>\n>> - [x] a @created:2020-01-01\n>> - b")
        )
    }
}
