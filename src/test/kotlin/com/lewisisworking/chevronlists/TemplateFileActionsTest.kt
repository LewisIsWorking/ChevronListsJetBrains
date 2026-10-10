/**
 * TemplateFileActionsTest.kt
 * Runs template import, export and delete in a headless IDE. The file
 * choosers cannot open headless, so the tests call each action's companion
 * function with the file it would have been given.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class TemplateFileActionsTest : BasePlatformTestCase() {

    private val saved get() = ChevronListsSettings.getInstance().state.templates

    override fun tearDown() {
        try {
            saved.clear()
        } finally {
            super.tearDown()
        }
    }

    fun `test the actions are registered`() {
        for (id in listOf("ImportTemplates", "ExportTemplates", "DeleteTemplate"))
            assertNotNull(id, ActionManager.getInstance().getAction("ChevronLists.$id"))
    }

    fun `test import saves every section, and export writes them back`() {
        val source = myFixture.addFileToProject("plans.md", "> Trip\r\n>> - tickets\r\n\r\n> Shop\r\n>> - milk\r\n").virtualFile
        ImportTemplatesAction.importFrom(project, source)
        assertEquals(listOf("Trip", "Shop"), saved.map { it.name })
        assertEquals("> \${1:Trip}\n>> - \${2:tickets}\n\$0", saved[0].body)

        val out = File(FileUtil.createTempDirectory("export", null), "templates.md")
        ExportTemplatesAction.exportTo(project, out.toPath())
        assertEquals("> Trip\n>> - tickets\n\n> Shop\n>> - milk\n", out.readText())
    }

    fun `test a file with no sections imports nothing`() {
        val source = myFixture.addFileToProject("none.md", ">> - stray\n").virtualFile
        ImportTemplatesAction.importFrom(project, source)
        assertTrue(saved.isEmpty())
    }

    fun `test export and delete with nothing saved leave everything alone`() {
        myFixture.testAction(ActionManager.getInstance().getAction("ChevronLists.ExportTemplates"))
        myFixture.testAction(ActionManager.getInstance().getAction("ChevronLists.DeleteTemplate"))
        assertTrue(saved.isEmpty())
    }

    fun `test delete removes only the chosen template`() {
        val keep = ChevronListsSettings.SavedTemplate("Keep", "", "> \${1:Keep}\n\$0")
        val drop = ChevronListsSettings.SavedTemplate("Drop", "", "> \${1:Drop}\n\$0")
        saved += listOf(keep, drop)
        DeleteTemplateAction.delete(project, drop)
        assertEquals(listOf(keep), saved.toList())
    }
}
