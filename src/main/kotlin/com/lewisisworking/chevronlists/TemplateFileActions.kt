/**
 * TemplateFileActions.kt
 * IntelliJ bridges for TemplateFiles.kt and the saved template list: import
 * templates from a markdown file, export the saved ones to a file, and delete
 * a saved template. The file choosers cannot open in a headless IDE, so each
 * action's work lives in a companion function the tests call directly.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.SimpleListCellRenderer
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JList

private val saved get() = ChevronListsSettings.getInstance().state.templates

private fun plural(n: Int) = "$n template${if (n == 1) "" else "s"}"

/** Enabled whenever a project is open: these work on settings and files, not the editor */
abstract class ProjectAction(text: String, description: String) : AnAction(text, description, null) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) { e.presentation.isEnabled = e.project != null }
}

class ImportTemplatesAction : ProjectAction(
    "CL: Import Templates from File",
    "Save every > section of a markdown file as a template"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("md").withTitle("Import Templates from File")
        FileChooser.chooseFile(descriptor, project, null) { importFrom(project, it) }
    }

    companion object {
        /** Saves every section of [file] as a template and reports how many */
        fun importFrom(project: Project, file: VirtualFile) {
            val imported = importTemplates(String(file.contentsToByteArray(), file.charset).replace("\r\n", "\n").split("\n"), file.name)
            if (imported.isEmpty()) {
                StatusBar.Info.set("CL: No sections found in ${file.name}", project)
                return
            }
            saved += imported.map { ChevronListsSettings.SavedTemplate(it.name, it.description, it.body) }
            StatusBar.Info.set("CL: Imported ${plural(imported.size)}", project)
        }
    }
}

class ExportTemplatesAction : ProjectAction(
    "CL: Export Templates to File",
    "Write the saved templates to a markdown file, one > section each"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (saved.isEmpty()) {
            StatusBar.Info.set("CL: No saved templates to export", project)
            return
        }
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
            FileSaverDescriptor("Export Templates to File", "Choose where to save the templates", *arrayOf("md")), project)
        val target = dialog.save("templates.md") ?: return
        exportTo(project, target.file.toPath())
    }

    companion object {
        /** Writes the saved templates to [path] and reports how many */
        fun exportTo(project: Project, path: Path) {
            Files.writeString(path, exportTemplates(saved.map { it.toTemplate() }))
            StatusBar.Info.set("CL: Exported ${plural(saved.size)}", project)
        }
    }
}

class DeleteTemplateAction : ProjectAction(
    "CL: Delete Saved Template",
    "Remove a template saved with Save Section as Template or Import Templates"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (saved.isEmpty()) {
            StatusBar.Info.set("CL: No saved templates to delete", project)
            return
        }
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(saved.toList())
            .setTitle("Delete Saved Template")
            .setRenderer(SavedTemplateRenderer)
            .setNamerForFiltering { it.name }
            .setItemChosenCallback { delete(project, it) }
            .createPopup()
            .showCenteredInCurrentWindow(project)
    }

    // A subclass rather than SimpleListCellRenderer.create(...): every create()
    // overload is marked for removal, which the Plugin Verifier reports.
    private object SavedTemplateRenderer : SimpleListCellRenderer<ChevronListsSettings.SavedTemplate>() {
        override fun customize(list: JList<out ChevronListsSettings.SavedTemplate>, value: ChevronListsSettings.SavedTemplate?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.let { "${it.name}  -  ${it.description}" } ?: ""
        }
    }

    companion object {
        /** Removes [template] from the saved list */
        fun delete(project: Project, template: ChevronListsSettings.SavedTemplate) {
            saved.remove(template)
            StatusBar.Info.set("CL: Template \"${template.name}\" deleted", project)
        }
    }
}
