/**
 * SetActions.kt
 * IntelliJ bridge for Sets.kt: Move to List moves the item at the caret into
 * another section, picked from a popup. Inside a `#set` the popup offers the
 * set's other sections; anywhere else, every other section.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

class MoveToListAction : MarkdownEditorAction(
    "CL: Move Item to List",
    "Move this item into another section; inside a #set heading, one of the set's other lists"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = editor.document.text.split("\n")
        val line    = editor.document.getLineNumber(editor.caretModel.offset)
        val targets = moveTargetsFor(lines, line, prefix)
        if (targets.isEmpty()) {
            StatusBar.Info.set("CL: Put the caret on a top-level item, in a file with another section", project)
            return
        }
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(targets)
            .setTitle("Move Item to List")
            .setRenderer(TargetRenderer)
            .setNamerForFiltering { it.name }
            .setItemChosenCallback { move(project, editor, line, it) }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    // A subclass rather than SimpleListCellRenderer.create(...), which is marked for removal
    private object TargetRenderer : SimpleListCellRenderer<MoveTarget>() {
        override fun customize(list: JList<out MoveTarget>, value: MoveTarget?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.name ?: ""
        }
    }

    companion object {
        private val prefix get() = ChevronListsSettings.getInstance().state.listPrefix

        /** Moves the item at [line] into [target]'s section, rewriting only what changed */
        fun move(project: Project, editor: Editor, line: Int, target: MoveTarget) {
            val document = editor.document
            val before   = document.text
            val after    = computeMoveToSection(before.split("\n"), line, target.header, prefix) ?: return
            val change   = minimalReplacement(before, after.joinToString("\n")) ?: return
            WriteCommandAction.runWriteCommandAction(project, "Move Item to List", null, {
                document.replaceString(change.start, change.end, change.text)
            })
            StatusBar.Info.set("CL: Moved to \"${target.name}\"", project)
        }
    }
}
