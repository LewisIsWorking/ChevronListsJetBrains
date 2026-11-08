/**
 * ItemBulkActionsTest.kt
 * Runs the stamp, bulk and paste actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.StringSelection
import java.time.LocalDate

class ItemBulkActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private val today get() = LocalDate.now().toString()

    private fun perform(id: String, initial: String, answer: String? = null): String {
        myFixture.configureByText("notes.md", initial)
        TestDialogManager.setTestInputDialog { answer }
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    private fun paste(id: String, initial: String, clip: String): String {
        CopyPasteManager.getInstance().setContents(StringSelection(clip))
        return perform(id, initial)
    }

    fun `test stamp item`() =
        assertEquals(">> - a @created:$today // n", perform("StampItem", ">> - <caret>a // n"))

    fun `test a stamped item stays as it is`() =
        assertEquals(">> - a @created:2020-01-01", perform("StampItem", ">> - <caret>a @created:2020-01-01"))

    fun `test stamp all items stamps only the unstamped`() = assertEquals(
        "> A\n>> - a @created:2020-01-01\n>> - b @created:$today\n> B\n>> - c",
        perform("StampAllItems", "> A\n>> - <caret>a @created:2020-01-01\n>> - b\n> B\n>> - c"),
    )

    fun `test bulk tag accepts a leading hash`() =
        assertEquals("> A\n>> - a #x", perform("BulkTagItems", "> A\n>> - <caret>a", "#x"))

    fun `test bulk set due date`() =
        assertEquals("> A\n>> - a @2026-02-02", perform("BulkSetDueDate", "> A\n>> - <caret>a", "2026-02-02"))

    fun `test set expiry on all items`() = assertEquals(
        "> A\n>> - a @expires:2026-02-02",
        perform("SetExpiryOnAllItems", "> A\n>> - <caret>a", "2026-02-02"),
    )

    fun `test paste as bullets goes below the caret`() =
        assertEquals("> A\n>> - x\n>> - y\n>> - b", paste("PasteAsBullets", "> <caret>A\n>> - b", "x\n- y"))

    fun `test paste as numbered continues the list`() =
        assertEquals("> A\n>> 1. a\n>> 2. x", paste("PasteAsNumbered", "> A\n>> 1. <caret>a", "x"))

    fun `test paste as section goes after the section`() =
        assertEquals("> A\n>> - a\n\n> N\n>> - x", paste("PasteAsSection", "> A\n>> - <caret>a", "N\nx"))
}
