/**
 * TemplateActions.kt
 * IntelliJ bridges for Templates.kt: insert a built-in or saved template as a
 * live template (Tab moves through its fields), and save the section at the
 * caret as a new template.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

/** Enabled in a markdown editor, as the other item and section actions are */
abstract class MarkdownEditorAction(text: String, description: String) : AnAction(text, description, null) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }
}

class InsertTemplateAction : MarkdownEditorAction(
    "CL: Insert Template",
    "Insert a built-in or saved section template at the caret; Tab moves through its fields"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(allTemplates())
            .setTitle("Insert Template")
            .setRenderer(TemplateRenderer)
            .setNamerForFiltering { "${it.name} ${it.description}" }
            .setItemChosenCallback { insert(project, editor, it) }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    // A subclass rather than SimpleListCellRenderer.create(...): every create()
    // overload is marked for removal, which the Plugin Verifier reports.
    private object TemplateRenderer : SimpleListCellRenderer<ChevronTemplate>() {
        override fun customize(list: JList<out ChevronTemplate>, value: ChevronTemplate?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.let { "${it.name}  -  ${it.description}" } ?: ""
        }
    }

    companion object {
        /** The built-in templates, then the saved ones */
        fun allTemplates(): List<ChevronTemplate> =
            BUILT_IN_TEMPLATES + ChevronListsSettings.getInstance().state.templates.map { it.toTemplate() }

        /** Starts [template] as a live template at the caret */
        fun insert(project: Project, editor: Editor, template: ChevronTemplate) {
            val live = liveTemplateText(parseSnippet(template.body))
            val t = TemplateManager.getInstance(project).createTemplate("", "", live.text)
            t.isToReformat = false
            for ((name, default) in live.variables) t.addVariable(name, ConstantNode(default), ConstantNode(default), true)
            TemplateManager.getInstance(project).startTemplate(editor, t)
        }
    }
}

class SaveSectionAsTemplateAction : MarkdownEditorAction(
    "CL: Save Section as Template",
    "Save the section at the caret as a template for CL: Insert Template"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = editor.document.text.split("\n")
        val header  = sectionHeaderLine(lines, editor.document.getLineNumber(editor.caretModel.offset))
        if (header == null) {
            StatusBar.Info.set("CL: No > section header at the caret", project)
            return
        }
        val name = Messages.showInputDialog(project, "Template name:", "Save Section as Template", null,
            parseHeader(lines[header])?.content.orEmpty(), null)?.trim()
        if (name.isNullOrEmpty()) return
        val description = Messages.showInputDialog(project, "Short description:", "Save Section as Template", null)
            ?: return
        val body = sectionToSnippetBody(lines, header, ChevronListsSettings.getInstance().state.listPrefix)
        ChevronListsSettings.getInstance().state.templates +=
            ChevronListsSettings.SavedTemplate(name, description.trim(), body)
        StatusBar.Info.set("CL: Template \"$name\" saved", project)
    }
}
