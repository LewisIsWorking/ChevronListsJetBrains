/**
 * SectionMarkerActionsTest.kt
 * Runs the section-wide marker commands and votes in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SectionMarkerActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestDialog(TestDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private val text get() = myFixture.editor.document.text

    private fun check(id: String, before: String, after: String) {
        myFixture.configureByText("notes.md", before)
        myFixture.performEditorAction("ChevronLists.$id")
        assertEquals(after, text)
    }

    fun `test mark all items done stays in the section`() =
        check("MarkAllItemsDone", "> A\n>> - <caret>a\n>> - [ ] b\n> B\n>> - c", "> A\n>> - [x] a\n>> - [x] b\n> B\n>> - c")

    fun `test mark all items undone`() =
        check("MarkAllItemsUndone", "> <caret>A\n>> - [x] a\n>> - b", "> A\n>> - [ ] a\n>> - b")

    fun `test remove all checkboxes`() =
        check("RemoveAllCheckboxes", "> A\n>> 1. <caret>[x] a", "> A\n>> 1. a")

    fun `test clear all due dates`() =
        check("ClearAllDueDates", "> A\n>> - <caret>a @2026-01-01", "> A\n>> - a")

    fun `test clear all priority`() =
        check("ClearAllPriority", "> A\n>> - <caret>!!! a", "> A\n>> - a")

    fun `test strip comments from section`() =
        check("StripCommentsFromSection", "> A\n>> - <caret>a // why", "> A\n>> - a")

    fun `test add and remove vote`() {
        check("AddVote", ">> - <caret>a +1", ">> - a +2")
        check("RemoveVote", ">> - <caret>a +1", ">> - a")
    }

    fun `test strip all metadata asks first`() {
        TestDialogManager.setTestDialog(TestDialog.OK)
        check("StripAllMetadata", "> A\n>> - <caret>[x] a #t +2", "> A\n>> - a")
    }

    fun `test declining strip all metadata changes nothing`() {
        TestDialogManager.setTestDialog(TestDialog.NO)
        check("StripAllMetadata", "> A\n>> - <caret>[x] a #t +2", "> A\n>> - [x] a #t +2")
    }
}
