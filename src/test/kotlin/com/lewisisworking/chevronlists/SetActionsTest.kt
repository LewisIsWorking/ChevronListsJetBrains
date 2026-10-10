/**
 * SetActionsTest.kt
 * Runs Move to List and the set warning in a headless IDE. The popup cannot
 * open headless, so the move test calls the action's companion function with
 * the target it would have been given.
 */
package com.lewisisworking.chevronlists

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SetActionsTest : BasePlatformTestCase() {

    private val doc = "# Bikes #set\n> Paid.\n>> 1. Lewis.\n\n> Hasn't paid.\n>> 1. Ethan.\n>> 2. Luke.\n\n> Not going.\n>> 1. Ethan.\n"

    fun `test the action is registered`() =
        assertNotNull(ActionManager.getInstance().getAction("ChevronLists.MoveToList"))

    fun `test an item in two lists of a set is warned about`() {
        myFixture.configureByText("bikes.md", doc)
        val warnings = myFixture.doHighlighting(HighlightSeverity.WARNING).map { it.description }
        assertTrue(warnings.toString(), warnings.any { it.startsWith("In this set more than once") })
    }

    fun `test move puts the item at the end of the chosen list`() {
        myFixture.configureByText("bikes.md", doc)
        MoveToListAction.move(project, myFixture.editor, 6, MoveTarget(1, "Paid."))
        assertEquals("# Bikes #set\n> Paid.\n>> 1. Lewis.\n>> 2. Luke.\n\n> Hasn't paid.\n>> 1. Ethan.\n\n> Not going.\n>> 1. Ethan.\n",
            myFixture.editor.document.text)
    }

    fun `test the action on a line that is not an item changes nothing`() {
        myFixture.configureByText("bikes.md", "> Paid.<caret>\n>> 1. Lewis.\n> Owed\n")
        myFixture.testAction(ActionManager.getInstance().getAction("ChevronLists.MoveToList"))
        assertEquals("> Paid.\n>> 1. Lewis.\n> Owed\n", myFixture.editor.document.text)
    }

    fun `test the totals inlay is offered for markdown files only`() {
        val provider = SetTotalsInlayProvider()
        myFixture.configureByText("bikes.md", doc)
        assertNotNull(provider.createCollector(myFixture.file, myFixture.editor))
        myFixture.configureByText("bikes.txt", doc)
        assertNull(provider.createCollector(myFixture.file, myFixture.editor))
    }

    fun `test the totals sit at the end of the heading's line`() =
        assertEquals(4, endOfLine(4).line)
}
