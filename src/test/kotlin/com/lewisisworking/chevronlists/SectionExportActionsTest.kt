/**
 * SectionExportActionsTest.kt
 * Runs the Copy Section as HTML, JSON and CSV Row actions in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor

class SectionExportActionsTest : BasePlatformTestCase() {

    private fun copied(id: String): String {
        myFixture.configureByText("notes.md", "> A\n>> - <caret>a, b")
        myFixture.performEditorAction("ChevronLists.$id")
        return CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor) as String
    }

    fun `test copy as csv row`() = assertEquals("\"a, b\"", copied("CopySectionAsCsvRow"))

    fun `test copy as html`() = assertTrue(copied("CopySectionAsHtml").startsWith("<section>"))

    fun `test copy as json`() = assertTrue(copied("CopySectionAsJson").contains("\"section\": \"A\""))
}
