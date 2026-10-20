/**
 * SectionCopyActions.kt
 * IntelliJ bridge for SectionCopy.kt: copies the caret section to the clipboard
 * as standard Markdown or as plain text.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

abstract class ChevronCopySectionAction(
    actionText:        String,
    actionDescription: String,
    private val done:  String,
    private val format: (List<String>, Int, String) -> String?
) : MarkdownEditorAction(actionText, actionDescription) {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val text   = format(editor.document.text.split("\n"), editor.caretModel.logicalPosition.line, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "CL: No section found at the caret")
        CopyPasteManager.getInstance().setContents(StringSelection(text))
        HintManager.getInstance().showInformationHint(editor, done)
    }
}

class CopySectionAsMarkdownAction : ChevronCopySectionAction(
    actionText        = "CL: Copy Section as Markdown",
    actionDescription = "Copy the caret section as a Markdown heading and list",
    done              = "CL: Section copied as Markdown",
    format            = ::sectionAsMarkdown
)

class CopySectionAsPlainTextAction : ChevronCopySectionAction(
    actionText        = "CL: Copy Section as Plain Text",
    actionDescription = "Copy the caret section's header and item texts, without chevrons or bullets",
    done              = "CL: Section copied as plain text",
    format            = ::sectionAsPlainText
)
