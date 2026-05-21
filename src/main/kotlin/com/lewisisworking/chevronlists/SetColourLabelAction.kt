/**
 * SetColourLabelAction.kt
 * Opens a popup with the six available colour labels (red, green, blue, yellow,
 * orange, purple). The chosen label is applied to the current chevron item, or
 * to every chevron item in the current selection (any existing colour label on
 * a line is replaced).
 *
 * Distinct from ChevronLineTransformAction subclasses because it needs UI
 * interaction (the popup) before the transform can be computed.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.TextRange

class SetColourLabelAction : AnAction(
    "CL: Set Colour Label",
    "Apply a {red}/{green}/{blue}/{yellow}/{orange}/{purple} label to this item",
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

        val document = editor.document
        val caret    = editor.caretModel.currentCaret
        val (startLine, endLine) = if (caret.hasSelection()) {
            Pair(document.getLineNumber(caret.selectionStart),
                 document.getLineNumber(caret.selectionEnd))
        } else {
            val n = document.getLineNumber(caret.offset)
            Pair(n, n)
        }

        val popup = JBPopupFactory.getInstance().createListPopup(
            ColourLabelPopupStep(editor, project, startLine, endLine)
        )
        popup.showInBestPositionFor(editor)
    }

    private class ColourLabelPopupStep(
        private val editor:    Editor,
        private val project:   Project,
        private val startLine: Int,
        private val endLine:   Int
    ) : BaseListPopupStep<ColourLabel>("Set Colour Label", ColourLabel.values().toList()) {

        override fun onChosen(selected: ColourLabel?, finalChoice: Boolean): PopupStep<*>? {
            if (selected != null) applyLabel(selected)
            return FINAL_CHOICE
        }

        override fun getTextFor(value: ColourLabel): String = value.token

        private fun applyLabel(label: ColourLabel) {
            val document = editor.document
            val prefix   = ChevronListsSettings.getInstance().state.listPrefix
            WriteCommandAction.runWriteCommandAction(project) {
                for (line in endLine downTo startLine) {
                    val lineStart = document.getLineStartOffset(line)
                    val lineEnd   = document.getLineEndOffset(line)
                    val lineText  = document.getText(TextRange(lineStart, lineEnd))
                    val newLine   = computeSetColourLabel(lineText, prefix, label) ?: continue
                    document.replaceString(lineStart, lineEnd, newLine)
                }
            }
        }
    }
}