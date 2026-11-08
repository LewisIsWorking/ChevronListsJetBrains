/**
 * ItemEditActionsTest.kt
 * Runs merge, split, section sort and text style actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ItemEditActionsTest : BasePlatformTestCase() {

    private fun check(id: String, before: String, after: String) {
        myFixture.configureByText("notes.md", before)
        myFixture.performEditorAction("ChevronLists.$id")
        assertEquals(after, myFixture.editor.document.text)
    }

    fun `test merge with next`() {
        myFixture.configureByText("notes.md", ">> 1. <caret>a\n>> 2. b\n>> 3. c")
        myFixture.performEditorAction("ChevronLists.MergeItemWithNext")
        myFixture.checkResult(">> 1. <caret>a - b\n>> 2. c")
    }

    fun `test split puts the caret on the second item`() {
        myFixture.configureByText("notes.md", ">> - one <caret>two")
        myFixture.performEditorAction("ChevronLists.SplitItemAtCursor")
        myFixture.checkResult(">> - one\n>> - <caret>two")
    }

    fun `test sort sections`() {
        check("SortAllSectionsAscending", "> B\n>> - <caret>b\n\n> A", "> A\n\n> B\n>> - b")
        check("SortAllSectionsDescending", "> A\n> <caret>B", "> B\n> A")
    }

    fun `test bold wraps the word at the caret`() =
        check("BoldText", "one t<caret>wo", "one **two**")

    fun `test the styles wrap a selection`() =
        check("ItalicText", "<selection>a b</selection>", "_a b_")

    fun `test mono strikethrough and underline`() {
        check("MonoText", "<caret>a", "`a`")
        check("StrikethroughText", "<caret>a", "~~a~~")
        check("UnderlineText", "<caret>a", "a̲")
    }
}
