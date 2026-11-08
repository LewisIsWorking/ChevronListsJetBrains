/**
 * TagRewriteActionsTest.kt
 * Runs the tag, bullet, grouping and word goal actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TagRewriteActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun act(id: String, initial: String, vararg answers: String): String {
        val queue = ArrayDeque(answers.toList())
        TestDialogManager.setTestInputDialog { queue.removeFirst() }
        myFixture.configureByText("notes.md", initial)
        myFixture.performEditorAction("ChevronLists.$id")
        return myFixture.editor.document.text
    }

    fun `test rename tag across the file`() = assertEquals(
        "> A\n>> - #y\n> B\n>> - #y",
        act("RenameTag", "> A\n>> - <caret>#x\n> B\n>> - #x", "x", "y"),
    )

    fun `test rename tag in the section`() = assertEquals(
        "> A\n>> - #y\n> B\n>> - #x",
        act("RenameTagSection", "> A\n>> - <caret>#x\n> B\n>> - #x", "#x", "#y"),
    )

    fun `test change item prefix`() =
        assertEquals("> A\n>> * a", act("ChangeItemPrefix", "> A\n>> - <caret>a", "*"))

    fun `test group items by tag`() = assertEquals(
        "> A\n// #x\n>> - a #x\n// #y\n>> - b #y",
        act("GroupItemsByTag", "> A\n>> - <caret>a #x\n>> - b #y"),
    )

    fun `test set word count goal`() =
        assertEquals("> A ==500\n>> - a", act("SetWordCountGoal", "> A\n>> - <caret>a", "500"))

    fun `test a blank goal removes it`() =
        assertEquals("> A\n>> - a", act("SetWordCountGoal", "> A ==5\n>> - <caret>a", ""))
}
