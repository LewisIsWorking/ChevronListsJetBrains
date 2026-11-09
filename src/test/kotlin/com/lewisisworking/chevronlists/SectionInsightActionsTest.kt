/**
 * SectionInsightActionsTest.kt
 * Runs the section insight actions in a headless IDE. Actions that open a list
 * are run on files with nothing to list, as a popup cannot show here.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SectionInsightActionsTest : BasePlatformTestCase() {

    private val ids = listOf(
        "ShowSectionWeights", "ShowCompletionStreak", "SectionHealthCheck", "PinSectionToTop",
        "FindSimilarSections", "ShowProgressReport", "EvaluateExpression",
    )

    private fun runAction(id: String, text: String): String {
        myFixture.configureByText("notes.md", text)
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    fun `test every action is registered`() {
        for (id in ids) assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
    }

    fun `test reports with nothing to list leave the text alone`() {
        for (id in listOf("ShowSectionWeights", "ShowCompletionStreak", "FindSimilarSections", "ShowProgressReport")) {
            assertEquals("text", runAction(id, "<caret>text"))
        }
        assertEquals("> S\n>> - a", runAction("SectionHealthCheck", "<caret>> S\n>> - a"))
    }

    fun `test pin moves the section and the caret`() {
        assertEquals("> B\n> A", runAction("PinSectionToTop", "> A\n> B<caret>"))
        assertEquals(0, myFixture.editor.caretModel.logicalPosition.line)
    }

    fun `test pin leaves the first section alone`() {
        assertEquals("> A\n> B", runAction("PinSectionToTop", "> A<caret>\n> B"))
    }

    fun `test evaluate replaces the expression`() {
        assertEquals(">> - a =4", runAction("EvaluateExpression", ">> - a<caret> =2+2"))
    }

    fun `test evaluate leaves an item without one alone`() {
        assertEquals(">> - a", runAction("EvaluateExpression", ">> - a<caret>"))
    }
}
