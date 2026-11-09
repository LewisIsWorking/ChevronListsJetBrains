/**
 * QuickCaptureAction.kt
 * Quick Capture: asks for a section and some text, and adds it as a new item there.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages

class QuickCaptureAction : MarkdownEditorAction(
    "CL: Quick Capture", "Choose a section of this file and add a new item to the end of it"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val lines   = documentLines(editor)
        val names   = sectionNames(lines)
        if (names.isEmpty()) return HintManager.getInstance().showInformationHint(editor, "CL: No sections found in this file")
        val name = Messages.showInputDialog(project, "Capture to which section? (${names.joinToString(", ")})", "Quick Capture", null,
            names.first(), oneOfValidator(names.toSet()) { listOf(it.trim()) })?.trim() ?: return
        val header = sectionNamed(lines, name) ?: return
        val text   = Messages.showInputDialog(project, "Add item to \"$name\":", "Quick Capture", null)?.trim().orEmpty()
        if (text.isEmpty()) return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val after  = withBlockInSection(lines, header, listOf(">> $prefix $text"))
        WriteCommandAction.runWriteCommandAction(project, "Quick Capture", null, {
            replaceChangedLines(editor.document, lines, after)
        })
    }
}
