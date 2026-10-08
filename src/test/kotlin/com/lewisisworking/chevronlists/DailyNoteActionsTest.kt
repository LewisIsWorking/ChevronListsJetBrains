/**
 * DailyNoteActionsTest.kt
 * Runs the daily note actions in a headless IDE against a real temp folder:
 * the note file is created, reused, filled from the template, and items sent
 * to it land under > Inbox.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.time.LocalDate

class DailyNoteActionsTest : BasePlatformTestCase() {

    private lateinit var folder: File
    private val state get() = ChevronListsSettings.getInstance().state

    override fun setUp() {
        super.setUp()
        folder = FileUtil.createTempDirectory("daily", null)
        VfsRootAccess.allowRootAccess(testRootDisposable, folder.path)
        state.dailyNotesFolder = folder.path
    }

    override fun tearDown() {
        try {
            state.dailyNotesFolder  = ""
            state.dailyNoteTemplate = ""
        } finally {
            super.tearDown()
        }
    }

    private val today get() = LocalDate.now()
    private val noteFile get() = File(folder, dailyNoteFileName(today))

    /** The note's text as the IDE sees it (unsaved edits included) */
    private fun noteText(): String {
        val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(noteFile)!!
        return FileDocumentManager.getInstance().getDocument(vf)!!.text
    }

    private fun run(id: String) = myFixture.testAction(ActionManager.getInstance().getAction(id))

    fun `test open daily note creates today's note and puts the caret on its empty item`() {
        run("ChevronLists.OpenDailyNote")
        assertEquals("> $today\n>> - \n", noteFile.readText())
        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
        assertEquals(noteFile.name, FileDocumentManager.getInstance().getFile(editor.document)!!.name)
        assertEquals(1, editor.caretModel.logicalPosition.line)
        assertEquals(5, editor.caretModel.logicalPosition.column)
    }

    fun `test open daily note uses the template and never overwrites an existing note`() {
        state.dailyNoteTemplate = "# {{weekday}}"
        run("ChevronLists.OpenDailyNote")
        assertTrue(noteText().startsWith("# "))
        assertFalse(noteText().contains("{{weekday}}"))

        state.dailyNoteTemplate = "changed"
        run("ChevronLists.OpenDailyNote")
        assertFalse(noteText().contains("changed"))
    }

    fun `test send to daily note creates the note with an inbox`() {
        myFixture.configureByText("notes.md", "> Work\n>> - <caret>call Sam\n")
        run("ChevronLists.SendToDailyNote")
        assertEquals("# $today\n\n> Inbox\n>> - call Sam\n", noteText())
        // The source item stays where it was: send copies, it does not move
        assertEquals("> Work\n>> - call Sam\n", myFixture.editor.document.text)
    }

    fun `test send to daily note files the item first under the existing inbox`() {
        noteFile.writeText("> Plans\n>> - p\n> Inbox\n>> - old\n")
        myFixture.configureByText("notes.md", "> Work\n>> 2. <caret>new thing\n")
        run("ChevronLists.SendToDailyNote")
        assertEquals("> Plans\n>> - p\n> Inbox\n>> - new thing\n>> - old\n", noteText())
    }

    fun `test send to daily note ignores a line that is not an item`() {
        myFixture.configureByText("notes.md", "> <caret>Work\n")
        run("ChevronLists.SendToDailyNote")
        assertFalse(noteFile.exists())
    }
}
