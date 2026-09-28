/**
 * GoToSectionAction.kt
 * IntelliJ bridge for SectionEntries.kt: a searchable popup of the file's
 * sections; choosing one moves the caret to its header. Type to filter.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

class GoToSectionAction : AnAction(
    "CL: Go to Section",
    "Pick a > section from a searchable list and jump to it",
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
        val prefix  = ChevronListsSettings.getInstance().state.listPrefix
        val entries = sectionEntries(editor.document.text.split("\n"), prefix)
        if (entries.isEmpty()) return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(entries)
            .setTitle("Go to Section")
            .setRenderer(SectionRenderer)
            .setNamerForFiltering { it.label }
            .setItemChosenCallback { jumpTo(editor, it) }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    // A subclass rather than SimpleListCellRenderer.create(...): every create()
    // overload is marked for removal, which the Plugin Verifier reports.
    private object SectionRenderer : SimpleListCellRenderer<SectionEntry>() {
        override fun customize(list: JList<out SectionEntry>, value: SectionEntry?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.label ?: ""
        }
    }

    companion object {
        /** Moves the caret to the start of [entry]'s header and scrolls it into view */
        fun jumpTo(editor: Editor, entry: SectionEntry) {
            editor.caretModel.moveToLogicalPosition(LogicalPosition(entry.line, 0))
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER_UP)
        }
    }
}
