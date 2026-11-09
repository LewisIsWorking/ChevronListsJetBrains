/**
 * PasteExportPreviewActionsTest.kt
 * Runs Smart Paste, Copy Section As, Export to Obsidian, Preview Item and Text
 * Transform in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

class PasteExportPreviewActionsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun text() = myFixture.editor.document.text

    private fun answer(value: String) = TestDialogManager.setTestInputDialog { value }

    fun `test smart paste adds numbered items below the cursor`() {
        myFixture.configureByText("notes.md", "> A<caret>\n> B\n")
        CopyPasteManager.getInstance().setContents(StringSelection("1. x\n2. y"))
        myFixture.performEditorAction("ChevronLists.SmartPaste")
        assertEquals("> A\n>> 1. x\n>> 2. y\n> B\n", text())
    }

    fun `test a section is copied in the chosen format`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n")
        answer("CSV")
        myFixture.performEditorAction("ChevronLists.CopySectionAs")
        assertEquals("index,depth,content\n1,0,\"a\"", CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor))
    }

    fun `test previewing an item changes nothing`() {
        myFixture.configureByText("notes.md", ">> - a <b> #t<caret>\n")
        myFixture.performEditorAction("ChevronLists.PreviewItem")
        assertEquals(">> - a <b> #t\n", text())
    }

    fun `test a transform changes the case of the selection`() {
        myFixture.configureByText("notes.md", ">> - some <selection>words</selection> here\n")
        answer("UPPERCASE")
        myFixture.performEditorAction("ChevronLists.TextTransform")
        assertEquals(">> - some WORDS here\n", text())
    }

    fun `test with no selection a transform takes the word at the cursor`() {
        myFixture.configureByText("notes.md", ">> - some wo<caret>rds here\n")
        answer("Title Case")
        myFixture.performEditorAction("ChevronLists.TextTransform")
        assertEquals(">> - some Words here\n", text())
    }

    fun `test a transform can apply a text style`() {
        myFixture.configureByText("notes.md", ">> - <selection>word</selection>\n")
        answer("Bold")
        myFixture.performEditorAction("ChevronLists.TextTransform")
        assertEquals(">> - **word**\n", text())
    }

    fun `test the obsidian export opens as a new document`() {
        myFixture.configureByText("notes.md", "> A<caret>\n")
        myFixture.performEditorAction("ChevronLists.ExportToObsidian")
        val opened = FileEditorManager.getInstance(project).openFiles.first { it.name == "Obsidian export.md" }
        assertTrue(String(opened.contentsToByteArray()).contains("## A"))
    }
}
