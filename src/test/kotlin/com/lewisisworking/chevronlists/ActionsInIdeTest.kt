/**
 * ActionsInIdeTest.kt
 * Runs the plugin's actions inside a real (headless) IDE: a markdown file is
 * opened in an editor, the action is invoked by its plugin.xml id, and the
 * resulting text and caret are checked. The pure logic has its own unit tests;
 * these prove the IDE side - registration, caret handling, document writes and
 * the paste hook - actually works.
 */
package com.lewisisworking.chevronlists

import com.intellij.lang.folding.LanguageFolding
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.StringSelection

class ActionsInIdeTest : BasePlatformTestCase() {

    private fun open(text: String) {
        myFixture.configureByText("notes.md", text)
    }

    private fun run(actionId: String) {
        myFixture.performEditorAction(actionId)
    }

    // Move Item Up / Down
    fun `test move item down carries its children and follows the caret`() {
        open("> H\n>> - <caret>a\n>>> - a1\n>> - b")
        run("ChevronLists.MoveItemDown")
        myFixture.checkResult("> H\n>> - b\n>> - <caret>a\n>>> - a1")
    }

    fun `test move item up swaps with the previous sibling`() {
        open("> H\n>> - a\n>> - <caret>b")
        run("ChevronLists.MoveItemUp")
        myFixture.checkResult("> H\n>> - <caret>b\n>> - a")
    }

    fun `test move item does nothing for the first item`() {
        open("> H\n>> - <caret>a\n>> - b")
        run("ChevronLists.MoveItemUp")
        myFixture.checkResult("> H\n>> - <caret>a\n>> - b")
    }

    // Sort
    fun `test sort keeps children with their parent`() {
        open("> H\n>> - <caret>b\n>>> - x\n>> - a\n>>> - y")
        run("ChevronLists.SortItemsAscending")
        assertEquals("> H\n>> - a\n>>> - y\n>> - b\n>>> - x", myFixture.editor.document.text)
    }

    fun `test sort descending`() {
        open("> H\n>> - <caret>a\n>> - c\n>> - b")
        run("ChevronLists.SortItemsDescending")
        assertEquals("> H\n>> - c\n>> - b\n>> - a", myFixture.editor.document.text)
    }

    // Renumber and convert
    fun `test renumber counts nested lists separately`() {
        open("> H\n>> 3. <caret>a\n>>> 5. a1\n>> 7. b\n>>> 2. b1")
        run("ChevronLists.RenumberItems")
        assertEquals("> H\n>> 1. a\n>>> 1. a1\n>> 2. b\n>>> 1. b1", myFixture.editor.document.text)
    }

    fun `test bullets convert to numbers and back`() {
        open("> H\n>> - <caret>a\n>> - b")
        run("ChevronLists.ConvertBulletsToNumbered")
        assertEquals("> H\n>> 1. a\n>> 2. b", myFixture.editor.document.text)
        run("ChevronLists.ConvertNumberedToBullets")
        assertEquals("> H\n>> - a\n>> - b", myFixture.editor.document.text)
    }

    // Go to Section (the popup itself cannot open in a headless IDE)
    fun `test go to section is registered and jumps to the chosen header`() {
        assertNotNull(ActionManager.getInstance().getAction("ChevronLists.GoToSection"))
        open("> One\n>> - <caret>a\n> Two\n>> - b")
        val entries = sectionEntries(myFixture.editor.document.text.split("\n"), "-")
        assertEquals(listOf("One  (1 item)", "Two  (1 item)"), entries.map { it.label })
        GoToSectionAction.jumpTo(myFixture.editor, entries[1])
        assertEquals(LogicalPosition(2, 0), myFixture.editor.caretModel.logicalPosition)
    }

    // Section folding
    fun `test each section folds with its item count`() {
        open("> One\n>> - <caret>a\n>> - b\n\n> Two\n>> - c")
        val file = myFixture.file
        assertEquals("Markdown", file.language.id)
        // Registered for markdown files, alongside the Markdown plugin's own builder
        assertTrue(LanguageFolding.INSTANCE.allForLanguage(file.language).any { it is ChevronFoldingBuilder })

        val document = myFixture.editor.document
        val folds = ChevronFoldingBuilder().buildFoldRegions(file, document, false)
        assertEquals(listOf(" (2 items)", " (1 item)"), folds.map { it.placeholderText })
        // The first fold ends at "b", leaving the blank line before "> Two" visible
        assertEquals(document.getLineEndOffset(0), folds[0].range.startOffset)
        assertEquals(document.getLineEndOffset(2), folds[0].range.endOffset)
    }

    // Archive done items
    fun `test archive moves done blocks into a new archive`() {
        open("> Tasks\n>> - [x] <caret>a\n>>> - a1\n>> - [ ] b")
        run("ChevronLists.ArchiveDone")
        assertEquals("> Tasks\n>> - [ ] b\n\n> Archive\n>> - [x] a\n>>> - a1", myFixture.editor.document.text)
    }

    // Mark all done / undone
    fun `test mark all done and undone in the section`() {
        open("> H\n>> - [ ] <caret>a\n>> - b\n>> 2. [x] c\n> Other\n>> - [ ] z")
        run("ChevronLists.MarkAllDone")
        assertEquals("> H\n>> - [x] a\n>> - b\n>> 2. [x] c\n> Other\n>> - [ ] z", myFixture.editor.document.text)
        run("ChevronLists.MarkAllUndone")
        assertEquals("> H\n>> - [ ] a\n>> - b\n>> 2. [ ] c\n> Other\n>> - [ ] z", myFixture.editor.document.text)
    }

    // Duplicate
    fun `test duplicate copies the block below and renumbers the rest`() {
        open("> H\n>> 1. <caret>a\n>>> - a1\n>> 2. b")
        run("ChevronLists.DuplicateItem")
        myFixture.checkResult("> H\n>> 1. a\n>>> - a1\n>> 2. <caret>a\n>>> - a1\n>> 3. b")
    }

    // Header navigation
    fun `test jump to next and previous header`() {
        open("> One\n>> - <caret>a\n> Two\n>> - b")
        run("ChevronLists.JumpToNextHeader")
        assertEquals(2, myFixture.editor.caretModel.logicalPosition.line)
        run("ChevronLists.JumpToPreviousHeader")
        assertEquals(0, myFixture.editor.caretModel.logicalPosition.line)
    }

    // Paste lines as items (the copyPastePreProcessor)
    fun `test pasting several lines into an item makes each its own item`() {
        open("> H\n>> - <caret>")
        CopyPasteManager.getInstance().setContents(StringSelection("one\ntwo\nthree"))
        run(IdeActions.ACTION_EDITOR_PASTE)
        assertEquals("> H\n>> - one\n>> - two\n>> - three", myFixture.editor.document.text)
    }

    fun `test pasting a single line is an ordinary paste`() {
        open("> H\n>> - <caret>")
        CopyPasteManager.getInstance().setContents(StringSelection("just one"))
        run(IdeActions.ACTION_EDITOR_PASTE)
        assertEquals("> H\n>> - just one", myFixture.editor.document.text)
    }

    fun `test the paste setting turns it off`() {
        val state = ChevronListsSettings.getInstance().state
        state.pasteLinesAsItems = false
        try {
            open("> H\n>> - <caret>")
            CopyPasteManager.getInstance().setContents(StringSelection("one\ntwo"))
            run(IdeActions.ACTION_EDITOR_PASTE)
            // An ordinary paste: the IDE may indent the second line to the caret
            // column, but it must not become an item
            val lines = myFixture.editor.document.text.split("\n")
            assertEquals(listOf("> H", ">> - one", "two"), lines.map { it.trim() })
            assertNull(parseBullet(lines[2], "-"))
        } finally {
            state.pasteLinesAsItems = true
        }
    }

    // Toggle Done, the oldest action, as a smoke test of the existing bridges
    fun `test toggle done on the current item`() {
        open("> H\n>> - <caret>task")
        run("ChevronLists.ToggleDone")
        assertEquals("> H\n>> - [x] task", myFixture.editor.document.text)
    }
}
