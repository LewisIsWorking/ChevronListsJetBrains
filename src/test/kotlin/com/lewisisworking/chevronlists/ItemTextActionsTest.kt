/**
 * ItemTextActionsTest.kt
 * Proves the item text actions are registered and edit the document.
 */
package com.lewisisworking.chevronlists

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ItemTextActionsTest : BasePlatformTestCase() {

    private fun check(action: String, before: String, after: String) {
        myFixture.configureByText("notes.md", before)
        myFixture.performEditorAction(action)
        assertEquals(after, myFixture.editor.document.text)
    }

    fun `test uppercase`() = check("ChevronLists.UppercaseItem", ">> - {red} bu<caret>y #milk", ">> - {red} BUY #milk")

    fun `test lowercase`() = check("ChevronLists.LowercaseItem", ">> - BU<caret>Y @Lewis", ">> - buy @Lewis")

    fun `test title case`() = check("ChevronLists.TitleCaseItem", ">> 2. bu<caret>y milk", ">> 2. Buy Milk")

    fun `test strikethrough toggles`() {
        check("ChevronLists.StrikethroughItem", ">> - [x] do<caret>ne", ">> - [x] ~~done~~")
        myFixture.performEditorAction("ChevronLists.StrikethroughItem")
        assertEquals(">> - [x] done", myFixture.editor.document.text)
    }

    fun `test remove strikethrough`() =
        check("ChevronLists.RemoveStrikethrough", ">> - ~~ol<caret>d~~", ">> - old")

    fun `test a header is left alone`() =
        check("ChevronLists.UppercaseItem", "> Hea<caret>der", "> Header")

    fun `test every item in a selection changes`() {
        myFixture.configureByText("notes.md", "<selection>>> - a\n>> - b</selection>")
        myFixture.performEditorAction("ChevronLists.UppercaseItem")
        assertEquals(">> - A\n>> - B", myFixture.editor.document.text)
    }
}
