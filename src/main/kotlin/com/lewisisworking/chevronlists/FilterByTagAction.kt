/**
 * FilterByTagAction.kt
 * IntelliJ bridge for TagIndex.kt: pick a #tag (with its item count), then pick
 * one of the items carrying it and jump there. Both lists filter as you type.
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

class FilterByTagAction : AnAction(
    "CL: Filter by Tag",
    "Pick a #tag, then jump to one of the items that carry it",
    null
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val hits   = tagHits(editor.document.text.split("\n"), prefix)
        val counts = tagCounts(hits)
        if (counts.isEmpty()) return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(counts)
            .setTitle("Filter by Tag")
            .setRenderer(LabelRenderer { (tag, n) -> "#$tag  ($n item${if (n == 1) "" else "s"})" })
            .setNamerForFiltering { it.first }
            .setItemChosenCallback { (tag, _) -> showItems(editor, tag, hits.filter { it.tag == tag }) }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private fun showItems(editor: Editor, tag: String, items: List<TagHit>) {
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items)
            .setTitle("Items Tagged #$tag")
            .setRenderer(LabelRenderer { it.label })
            .setNamerForFiltering { it.label }
            .setItemChosenCallback { jumpTo(editor, it) }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    // A subclass rather than SimpleListCellRenderer.create(...), whose overloads
    // are marked for removal and flagged by the Plugin Verifier
    private class LabelRenderer<T>(private val labelOf: (T) -> String) : SimpleListCellRenderer<T>() {
        override fun customize(list: JList<out T>, value: T?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.let(labelOf) ?: ""
        }
    }

    companion object {
        /** Moves the caret to the tagged item and scrolls it into view */
        fun jumpTo(editor: Editor, hit: TagHit) {
            editor.caretModel.moveToLogicalPosition(LogicalPosition(hit.line, 0))
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER_UP)
        }
    }
}
