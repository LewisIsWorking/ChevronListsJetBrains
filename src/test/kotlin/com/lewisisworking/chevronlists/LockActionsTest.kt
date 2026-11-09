/**
 * LockActionsTest.kt
 * Runs Lock and Unlock Section in a headless IDE, and saves documents to check
 * the warning. An IDE can save on its own at any moment, so only a save that
 * changes a locked section may warn.
 */
package com.lewisisworking.chevronlists

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class LockActionsTest : BasePlatformTestCase() {

    private fun text() = myFixture.editor.document.text

    private fun warnings(): MutableList<String> {
        val seen = mutableListOf<String>()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) { seen += notification.content }
        })
        return seen
    }

    private fun append(document: Document, added: String) =
        WriteCommandAction.runWriteCommandAction(project) { document.insertString(document.textLength, added) }

    private fun save(document: Document) {
        FileDocumentManager.getInstance().saveDocument(document)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    fun `test locking adds the marker to the section at the cursor`() {
        myFixture.configureByText("notes.md", "> A\n>> - a<caret>\n> B\n")
        myFixture.performEditorAction("ChevronLists.LockSection")
        assertEquals("> A\n>> [locked]\n>> - a\n> B\n", text())
    }

    fun `test locking outside a section changes nothing`() {
        myFixture.configureByText("notes.md", "prose<caret>\n> A\n")
        myFixture.performEditorAction("ChevronLists.LockSection")
        assertEquals("prose\n> A\n", text())
    }

    fun `test unlocking removes the marker`() {
        myFixture.configureByText("notes.md", "> A\n>> [locked]\n>> - a<caret>\n")
        myFixture.performEditorAction("ChevronLists.UnlockSection")
        assertEquals("> A\n>> - a\n", text())
    }

    fun `test saving an edit to a locked section warns`() {
        val seen = warnings()
        myFixture.configureByText("notes.md", "> A\n>> [locked]\n>> - a\n")
        val document = myFixture.editor.document
        append(document, ">> - b\n")
        save(document)
        assertEquals(1, seen.size)
        assertTrue(seen.single(), seen.single().contains("\"A\""))
    }

    fun `test saving an edit elsewhere does not warn`() {
        val seen = warnings()
        myFixture.configureByText("notes.md", "> A\n>> [locked]\n>> - a\n> B\n")
        val document = myFixture.editor.document
        append(document, ">> - b\n")
        save(document)
        assertEquals(emptyList<String>(), seen)
    }

    fun `test saving again after the warning does not warn again`() {
        val seen = warnings()
        myFixture.configureByText("notes.md", "> A\n>> [locked]\n>> - a\n")
        val document = myFixture.editor.document
        append(document, ">> - b\n")
        save(document)
        append(document, "\n")
        save(document)
        assertEquals(1, seen.size)
    }

    fun `test the warning can unlock the section by name`() {
        myFixture.configureByText("notes.md", "> A\n>> - a\n> B\n>> [locked]\n>> - b\n")
        unlockSectionNamed(project, myFixture.editor.document, "B")
        assertEquals("> A\n>> - a\n> B\n>> - b\n", text())
    }
}
