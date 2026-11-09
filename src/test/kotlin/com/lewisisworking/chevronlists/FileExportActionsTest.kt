/**
 * FileExportActionsTest.kt
 * Runs the file export actions in a headless IDE. A save dialog cannot show
 * here, so the actions are run with nothing to export.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FileExportActionsTest : BasePlatformTestCase() {

    private val ids = listOf(
        "ExportFileAsJson", "ExportFileAsCsv", "ExportFileAsHtml", "ExportFileAsMarkdownDocument", "ExportAllSectionsAsJson",
    )

    fun `test every export action is registered`() {
        for (id in ids) assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
    }

    fun `test a file with nothing to export is left alone`() {
        for (id in ids) {
            myFixture.configureByText("notes.md", "just prose\n")
            myFixture.performEditorAction("ChevronLists.$id")
            assertEquals("just prose\n", myFixture.editor.document.text)
        }
    }
}
