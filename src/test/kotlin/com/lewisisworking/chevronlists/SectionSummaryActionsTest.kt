/**
 * SectionSummaryActionsTest.kt
 * Runs the section report and all-caret actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SectionSummaryActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun act(id: String, initial: String, answer: String = ""): String {
        TestDialogManager.setTestInputDialog { answer }
        myFixture.configureByText("notes.md", initial)
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    fun `test toggle done acts at every caret`() =
        assertEquals(">> - [x] a\n>> - b\n>> - [ ] c", act("ToggleDoneAllCursors", ">> - <caret>a\n>> - b\n>> - <caret>[x] c"))

    fun `test set priority acts at every caret`() =
        assertEquals(">> - !! a\n>> - b\n>> - !! c", act("SetPriorityAllCursors", ">> - <caret>a\n>> - b\n>> - <caret>c", "2"))

    fun `test reports leave the text alone`() {
        val text = "> A\n>> - <caret>~5m a"
        val plain = text.replace("<caret>", "")
        for (id in listOf("ShowNestingSummary", "ShowSectionTimeEstimate", "CountWordFrequency")) {
            assertEquals(plain, act(id, text))
        }
    }

    fun `test lists with nothing to show leave the text alone`() {
        for (id in listOf("ShowWordCount", "ShowPrioritySummary")) {
            assertEquals("text", act(id, "<caret>text"))
        }
    }

    fun `test list reports are registered`() {
        for (id in listOf("ShowWordCount", "ShowPrioritySummary")) {
            assertNotNull(ActionManager.getInstance().getAction("ChevronLists.$id"))
        }
    }
}
