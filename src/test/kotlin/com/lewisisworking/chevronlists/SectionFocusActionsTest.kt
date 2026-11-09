/**
 * SectionFocusActionsTest.kt
 * Runs Focus, Unfocus, Hide and Show Hidden Sections in a headless IDE. The fold
 * regions are added by hand, as the fold builder's would be.
 */
package com.lewisisworking.chevronlists

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SectionFocusActionsTest : BasePlatformTestCase() {

    private fun text() = myFixture.editor.document.text

    /** One fold per section, from its header to its last non-blank line */
    private fun foldSections() {
        val editor = myFixture.editor
        val lines  = editor.document.text.split("\n")
        editor.foldingModel.runBatchFoldingOperation {
            for (fold in sectionFolds(lines, "-")) {
                editor.foldingModel.addFoldRegion(
                    editor.document.getLineStartOffset(fold.headerLine),
                    editor.document.getLineEndOffset(fold.lastLine), lines[fold.headerLine])
            }
        }
    }

    /** The header of each section that is folded */
    private fun collapsed(): List<String> {
        val document = myFixture.editor.document
        return myFixture.editor.foldingModel.allFoldRegions.filter { !it.isExpanded }
            .map { document.text.substring(it.startOffset).substringBefore("\n") }
    }

    private fun setCollapsed(vararg headers: String) {
        val folding  = myFixture.editor.foldingModel
        val document = myFixture.editor.document
        folding.runBatchFoldingOperation {
            folding.allFoldRegions.forEach {
                it.isExpanded = document.text.substring(it.startOffset).substringBefore("\n") !in headers
            }
        }
    }

    fun `test focus folds every section but the one at the cursor`() {
        myFixture.configureByText("notes.md", "> A\n>> - a\n\n> B\n>> - b<caret>\n\n> C\n>> - c\n")
        foldSections()
        myFixture.performEditorAction("ChevronLists.FocusSection")
        assertEquals(listOf("> A", "> C"), collapsed())
    }

    fun `test focus opens the section at the cursor if it was folded`() {
        myFixture.configureByText("notes.md", "> A\n>> - a\n\n> B\n>> - b\n")
        foldSections()
        setCollapsed("> A", "> B")
        myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.text.indexOf("> B"))
        myFixture.performEditorAction("ChevronLists.FocusSection")
        assertEquals(listOf("> A"), collapsed())
    }

    fun `test focus outside a section folds nothing`() {
        myFixture.configureByText("notes.md", "prose<caret>\n> A\n>> - a\n")
        foldSections()
        myFixture.performEditorAction("ChevronLists.FocusSection")
        assertEquals(emptyList<String>(), collapsed())
    }

    fun `test unfocus opens every section`() {
        myFixture.configureByText("notes.md", "> A\n>> - a\n\n> B\n>> - b<caret>\n")
        foldSections()
        setCollapsed("> A")
        myFixture.performEditorAction("ChevronLists.UnfocusSection")
        assertEquals(emptyList<String>(), collapsed())
    }

    fun `test hide adds the marker and folds that section only`() {
        myFixture.configureByText("notes.md", "> A\n>> - a<caret>\n\n> B\n>> - b\n")
        foldSections()
        myFixture.performEditorAction("ChevronLists.HideSection")
        assertEquals("> A\n>> [hidden]\n>> - a\n\n> B\n>> - b\n", text())
        assertEquals(listOf("> A"), collapsed())
    }

    fun `test hiding a hidden section changes nothing`() {
        myFixture.configureByText("notes.md", "> A\n>> [hidden]\n>> - a<caret>\n")
        myFixture.performEditorAction("ChevronLists.HideSection")
        assertEquals("> A\n>> [hidden]\n>> - a\n", text())
    }

    fun `test hide outside a section changes nothing`() {
        myFixture.configureByText("notes.md", "prose<caret>\n> A\n")
        myFixture.performEditorAction("ChevronLists.HideSection")
        assertEquals("prose\n> A\n", text())
    }

    fun `test show hidden removes every marker and opens only those sections`() {
        myFixture.configureByText("notes.md", "> A\n>> [hidden]\n>> - a\n\n> B\n>> - b\n\n> C\n>> [hidden]\n>> - c<caret>")
        foldSections()
        setCollapsed("> A", "> B", "> C")
        myFixture.performEditorAction("ChevronLists.ShowHiddenSections")
        assertEquals("> A\n>> - a\n\n> B\n>> - b\n\n> C\n>> - c", text())
        assertEquals(listOf("> B"), collapsed())
    }

    fun `test show hidden with none hidden changes nothing`() {
        myFixture.configureByText("notes.md", "> A<caret>\n>> - a\n")
        myFixture.performEditorAction("ChevronLists.ShowHiddenSections")
        assertEquals("> A\n>> - a\n", text())
    }

    fun `test a hidden section starts folded when the file is opened`() {
        myFixture.configureByText("notes.md", "> A\n>> [hidden]\n>> - a\n\n> B\n>> - b\n")
        val builder = ChevronFoldingBuilder()
        val folds   = builder.buildFoldRegions(myFixture.file, myFixture.editor.document, false)
        assertEquals(listOf(true, false), folds.map { builder.isCollapsedByDefault(it) })
    }
}
