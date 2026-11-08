/**
 * ItemLabelActionsTest.kt
 * Runs the rating, priority, URL and clone actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor

class ItemLabelActionsTest : BasePlatformTestCase() {

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

    fun `test set item rating`() =
        assertEquals("> A\n>> - ★4 a\n>> - b", act("SetItemRating", "> A\n>> - <caret>a\n>> - b", "4"))

    fun `test bulk set rating`() =
        assertEquals("> A\n>> - ★2 a\n>> 1. ★2 b\n> B\n>> - c", act("BulkSetRating", "> A\n>> - <caret>★5 a\n>> 1. b\n> B\n>> - c", "2"))

    fun `test bulk set rating zero removes them`() =
        assertEquals("> A\n>> - a", act("BulkSetRating", "> A\n>> - <caret>★5 a", "0"))

    fun `test set item priority`() =
        assertEquals(">> - [x] !! a", act("SetItemPriority", ">> - <caret>[x] a", "2"))

    fun `test bulk set priority`() =
        assertEquals("> A\n>> - !!! a\n>> - !!! b", act("BulkSetPriority", "> A\n>> - <caret>a\n>> - ! b", "3"))

    fun `test a rating needs an item`() = assertEquals("text", act("SetItemRating", "<caret>text", "4"))

    fun `test clone item to section moves the caret to the copy`() {
        assertEquals("> A\n>> - a\n> B\n>> - b\n>> - a", act("CloneItemToSection", "> A\n>> - <caret>a\n> B\n>> - b", "b"))
        assertEquals(4, myFixture.editor.caretModel.logicalPosition.line)
    }

    fun `test extract urls copies them`() {
        act("ExtractUrlsFromSection", "> A\n>> - <caret>https://a.example and https://b.example.")
        assertEquals("https://a.example\nhttps://b.example", CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)!!)
    }
}
