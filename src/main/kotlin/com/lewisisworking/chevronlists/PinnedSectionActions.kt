/**
 * PinnedSectionActions.kt
 * IntelliJ bridge for PinnedSections.kt: Toggle Pin pins the caret section by
 * name (kept per project, matched case-insensitively), and Filter Pinned
 * Sections offers every pinned section in the file from a searchable popup.
 * Ported from the VS Code extension's pinCommands.ts.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

private const val PINNED_KEY = "chevronLists.pinnedSections"

/** The pinned section names for [project], lowercased like the VS Code extension */
fun pinnedNames(project: Project): MutableSet<String> =
    PropertiesComponent.getInstance(project).getValues(PINNED_KEY)?.toMutableSet() ?: mutableSetOf()

/** Saves [pins] as the pinned section names for [project] */
fun savePinnedNames(project: Project, pins: Set<String>) {
    PropertiesComponent.getInstance(project).setValues(PINNED_KEY, pins.toTypedArray())
}

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

class TogglePinSectionAction : MarkdownEditorAction(
    "CL: Toggle Pin",
    "Pin the section at the caret by name, so CL: Filter Pinned Sections can jump to it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val (_, name) = sectionNameAt(documentLines(editor), caretLine(editor))
            ?: return tell(editor, "CL: No section header found at cursor")
        val pins = togglePinned(pinnedNames(project), name)
        savePinnedNames(project, pins)
        tell(editor, if (pinKey(name) in pins) "CL: Pinned \"$name\"" else "CL: Unpinned \"$name\"")
    }
}

class FilterPinnedSectionsAction : AnAction(
    "CL: Filter Pinned Sections",
    "Pick a pinned section from a searchable list and jump to it",
    null
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val pins    = pinnedSections(documentLines(editor), pinnedNames(project))
        if (pins.isEmpty()) return tell(editor, "CL: No pinned sections - use CL: Toggle Pin to pin a section")
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(pins)
            .setTitle("Jump to a pinned section")
            .setRenderer(PinRenderer)
            .setNamerForFiltering { it.name }
            .setItemChosenCallback {
                editor.caretModel.moveToLogicalPosition(LogicalPosition(it.header, 0))
                editor.scrollingModel.scrollToCaret(ScrollType.CENTER_UP)
            }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private object PinRenderer : SimpleListCellRenderer<PinnedSection>() {
        override fun customize(list: JList<out PinnedSection>, value: PinnedSection?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.let { "📌 ${it.name}" } ?: ""
        }
    }
}
