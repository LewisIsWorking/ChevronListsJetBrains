/**
 * ItemCloneActionsTest.kt
 * Proves the item clone actions are registered, edit the document and land the
 * caret on the copy.
 */
package com.lewisisworking.chevronlists

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ItemCloneActionsTest : BasePlatformTestCase() {

    private fun check(action: String, before: String, after: String) {
        myFixture.configureByText("notes.md", before)
        myFixture.performEditorAction(action)
        myFixture.checkResult(after)
    }

    fun `test clone item goes to the end of the section`() =
        check("ChevronLists.CloneItem", "> A\n>> - <caret>a\n>> - b\n\n> B", "> A\n>> - a\n>> - b\n>> - <caret>a\n\n> B")

    fun `test clone as done`() =
        check("ChevronLists.CloneItemAsDone", ">> - <caret>a\n>> - b", ">> - a\n>> - <caret>[x] a\n>> - b")

    fun `test clone stripped`() =
        check("ChevronLists.CloneItemStripped", ">> - <caret>a #tag", ">> - a #tag\n>> - <caret>a")

    fun `test duplicate and increment`() =
        check("ChevronLists.DuplicateItemAndIncrement", ">> - <caret>step 1", ">> - step 1\n>> - <caret>step 2")

    fun `test a header is left alone`() =
        check("ChevronLists.CloneItemAsDone", "> <caret>A", "> <caret>A")
}
