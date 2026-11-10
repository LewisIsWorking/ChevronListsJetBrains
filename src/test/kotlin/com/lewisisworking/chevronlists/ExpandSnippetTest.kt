/**
 * ExpandSnippetTest.kt
 * Expands the chl and chn snippet triggers in a headless IDE.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExpandSnippetTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        TemplateManagerImpl.setTemplateTesting(testRootDisposable)
    }

    override fun tearDown() {
        try {
            ChevronListsSettings.getInstance().state.listPrefix = "-"
        } finally {
            super.tearDown()
        }
    }

    private fun expand(body: String): String {
        myFixture.configureByText("notes.md", body)
        myFixture.performEditorAction("ChevronLists.ExpandSnippet")
        return myFixture.editor.document.text
    }

    fun `test chl becomes a bullet list`() {
        assertEquals("> Section Header\n>> - First item\n>> - Second item\n>> - ", expand("chl<caret>"))
    }

    fun `test chn becomes a numbered list`() {
        assertEquals("> Section Header\n>> 1. First item\n>> 2. Second item\n>> 3. ", expand("chn<caret>"))
    }

    fun `test the list prefix setting is used`() {
        ChevronListsSettings.getInstance().state.listPrefix = "*"
        assertEquals("> Section Header\n>> * First item\n>> * Second item\n>> * ", expand("chl<caret>"))
    }

    fun `test text around the trigger stays`() {
        assertEquals("a\n> Section Header\n>> - First item\n>> - Second item\n>> - \nb", expand("a\nchl<caret>\nb"))
    }

    fun `test an indented trigger expands`() {
        assertEquals("  > Section Header\n>> - First item\n>> - Second item\n>> - ", expand("  chl<caret>"))
    }

    fun `test the trigger must be the whole text before the caret`() {
        assertEquals("xchl", expand("xchl<caret>"))
        assertEquals("a chl", expand("a chl<caret>"))
    }

    fun `test text after the caret does not count`() {
        assertEquals("ch", expand("ch<caret>"))
    }

    fun `test the caret must follow the trigger`() {
        assertEquals("chl", expand("<caret>chl"))
    }
}
