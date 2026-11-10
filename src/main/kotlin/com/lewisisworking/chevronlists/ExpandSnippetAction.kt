/**
 * ExpandSnippetAction.kt
 * Expand Snippet: turns a chl or chn typed before the caret into a section with a list.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange

private fun snippetBody(trigger: String, prefix: String): String? = when (trigger) {
    "chl" -> "> \${1:Section Header}\n>> $prefix \${2:First item}\n>> $prefix \${3:Second item}\n>> $prefix \$0"
    "chn" -> "> \${1:Section Header}\n>> 1. \${2:First item}\n>> 2. \${3:Second item}\n>> 3. \$0"
    else  -> null
}

class ExpandSnippetAction : MarkdownEditorAction(
    "CL: Expand Snippet", "Turn chl or chn before the caret into a section with a bullet or numbered list"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor   = e.getData(CommonDataKeys.EDITOR) ?: return
        val project  = e.project ?: return
        val document = editor.document
        val caret    = editor.caretModel.offset
        val before   = document.getText(TextRange(document.getLineStartOffset(document.getLineNumber(caret)), caret))
        val trigger  = before.trimStart()
        val body     = snippetBody(trigger, ChevronListsSettings.getInstance().state.listPrefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: Type chl or chn before the caret to expand a snippet")
        WriteCommandAction.runWriteCommandAction(project, "Expand Snippet", null, {
            document.deleteString(caret - trigger.length, caret)
            InsertTemplateAction.insert(project, editor, ChevronTemplate("", "", body))
        })
    }
}
