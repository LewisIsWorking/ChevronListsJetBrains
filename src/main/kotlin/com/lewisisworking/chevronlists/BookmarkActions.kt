/**
 * BookmarkActions.kt
 * Thin actions over Bookmarks.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages

class AddBookmarkAction : MarkdownEditorAction(
    "CL: Add Bookmark", "Add a named bookmark above the cursor line"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val name   = Messages.showInputDialog(e.project, "Bookmark name:", "Add Bookmark", null) ?: return
        val marker = bookmarkMarker(name) ?: return
        WriteCommandAction.runWriteCommandAction(e.project, "Add Bookmark", null, {
            val document = editor.document
            document.insertString(document.getLineStartOffset(editor.caretModel.logicalPosition.line), marker + "\n")
        })
    }
}

class JumpToBookmarkAction : MarkdownEditorAction(
    "CL: Jump to Bookmark", "List the bookmarks of this file and jump to one"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Jump to a bookmark", bookmarkEntries(documentLines(editor)),
            "CL: No bookmarks found (use CL: Add Bookmark to add one)")
    }
}

class RemoveBookmarkAction : MarkdownEditorAction(
    "CL: Remove Bookmark", "Remove the bookmark on the cursor line"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = documentLines(editor)
        val line   = editor.caretModel.logicalPosition.line
        if (parseBookmark(lines[line]) == null) {
            return HintManager.getInstance().showInformationHint(editor, "CL: No bookmark found on this line")
        }
        WriteCommandAction.runWriteCommandAction(e.project, "Remove Bookmark", null, {
            replaceChangedLines(editor.document, lines, lines.filterIndexed { index, _ -> index != line })
        })
    }
}
