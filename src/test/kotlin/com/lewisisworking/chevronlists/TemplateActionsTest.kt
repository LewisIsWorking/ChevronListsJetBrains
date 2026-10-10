/**
 * TemplateActionsTest.kt
 * Runs the template actions in a headless IDE: a template starts as a live
 * template with its defaults filled in, and Save Section as Template stores a
 * template that inserts the section back.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TemplateActionsTest : BasePlatformTestCase() {

    private val state get() = ChevronListsSettings.getInstance().state

    override fun setUp() {
        super.setUp()
        TemplateManagerImpl.setTemplateTesting(testRootDisposable)
    }

    override fun tearDown() {
        try {
            state.templates.clear()
            TestDialogManager.setTestInputDialog(TestInputDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    private fun run(id: String) = myFixture.testAction(ActionManager.getInstance().getAction(id))

    private fun insert(template: ChevronTemplate) =
        InsertTemplateAction.insert(project, myFixture.editor, template)

    fun `test the actions are registered`() {
        assertNotNull(ActionManager.getInstance().getAction("ChevronLists.InsertTemplate"))
        assertNotNull(ActionManager.getInstance().getAction("ChevronLists.SaveSectionAsTemplate"))
    }

    fun `test a built-in template inserts with its defaults at the caret`() {
        myFixture.configureByText("notes.md", "intro\n<caret>")
        insert(BUILT_IN_TEMPLATES.first { it.name == "Bullet List" })
        assertEquals("intro\n> Section Header\n>> - First item\n>> - Second item\n>> - ", myFixture.editor.document.text)
    }

    fun `test literal dollar signs survive insertion`() {
        myFixture.configureByText("notes.md", "<caret>")
        insert(ChevronTemplate("Prices", "", "> \${1:Shop}\n>> - milk \\\$2 each\n\$0"))
        assertEquals("> Shop\n>> - milk \$2 each\n", myFixture.editor.document.text)
    }

    fun `test save section as template stores it, and it inserts the section back`() {
        myFixture.configureByText("notes.md", "> Trip\n>> - <caret>tickets\n>> 2. hotel\n\n> Other\n>> - x")
        val answers = ArrayDeque(listOf("Trip plan", "  for holidays "))
        TestDialogManager.setTestInputDialog { answers.removeFirst() }
        run("ChevronLists.SaveSectionAsTemplate")

        val saved = state.templates.single()
        assertEquals("Trip plan", saved.name)
        assertEquals("for holidays", saved.description)
        assertEquals(saved.toTemplate(), InsertTemplateAction.allTemplates().last())

        myFixture.configureByText("other.md", "<caret>")
        insert(saved.toTemplate())
        assertEquals("> Trip\n>> - tickets\n>> 2. hotel\n", myFixture.editor.document.text)
    }

    fun `test cancelling the name saves nothing`() {
        myFixture.configureByText("notes.md", "> Trip\n>> - <caret>tickets")
        TestDialogManager.setTestInputDialog { null }
        run("ChevronLists.SaveSectionAsTemplate")
        assertTrue(state.templates.isEmpty())
    }

    fun `test with no section header nothing is asked or saved`() {
        myFixture.configureByText("notes.md", "just <caret>text")
        TestDialogManager.setTestInputDialog { throw AssertionError("should not ask") }
        run("ChevronLists.SaveSectionAsTemplate")
        assertTrue(state.templates.isEmpty())
    }
}
