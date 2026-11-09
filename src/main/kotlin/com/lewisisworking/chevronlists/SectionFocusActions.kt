/**
 * SectionFocusActions.kt
 * Focus, Unfocus, Hide and Show Hidden Sections, over the plugin's section folds.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor

private fun tell(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

class FocusSectionAction : MarkdownEditorAction(
    "CL: Focus on Section", "Fold every section except the one at the cursor"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = documentLines(editor)
        val header = headerAtOrAbove(lines, caretLine(editor)) ?: return tell(editor, "CL: No section found at cursor")
        val first  = editor.document.getLineStartOffset(header)
        val last   = editor.document.getLineEndOffset(sectionLastLine(lines, header))
        val folding = editor.foldingModel
        folding.runBatchFoldingOperation {
            folding.allFoldRegions.forEach { it.isExpanded = it.startOffset <= last && it.endOffset >= first }
        }
    }
}

class UnfocusSectionAction : SetAllFoldsAction("CL: Unfocus Section", "Unfold every section", true)

class HideSectionAction : MarkdownEditorAction(
    "CL: Hide Section", "Mark this section as hidden and fold it; it starts folded whenever the file is opened"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = documentLines(editor)
        val header = headerAtOrAbove(lines, caretLine(editor)) ?: return tell(editor, "CL: No section found at cursor")
        if (isSectionHidden(lines, header)) return tell(editor, "CL: Section is already hidden")
        val document = editor.document
        WriteCommandAction.runWriteCommandAction(e.project, "Hide Section", null, {
            document.insertString(document.getLineEndOffset(header), "\n$HIDDEN_MARKER")
        })
        val start   = document.getLineStartOffset(header)
        val folding = editor.foldingModel
        folding.runBatchFoldingOperation {
            folding.allFoldRegions.filter { it.startOffset == start }.forEach { it.isExpanded = false }
        }
        tell(editor, "CL: Section hidden - use CL: Show Hidden Sections to reveal")
    }
}

/** Deletes [line] and one line break, so that no empty line is left behind */
private fun deleteLine(document: Document, line: Int) {
    val start = document.getLineStartOffset(line)
    val end   = document.getLineEndOffset(line)
    if (line + 1 < document.lineCount) document.deleteString(start, document.getLineStartOffset(line + 1))
    else document.deleteString(if (line > 0) document.getLineEndOffset(line - 1) else start, end)
}

class ShowHiddenSectionsAction : MarkdownEditorAction(
    "CL: Show Hidden Sections", "Remove every hidden marker in this file and unfold those sections"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines   = documentLines(editor)
        val markers = hiddenMarkerLines(lines)
        if (markers.isEmpty()) return tell(editor, "CL: No hidden sections found")
        val document = editor.document
        val headers  = markers.mapNotNull { headerAtOrAbove(lines, it) }.map(document::getLineStartOffset).toSet()
        val folding  = editor.foldingModel
        val regions  = folding.allFoldRegions.filter { it.startOffset in headers }
        WriteCommandAction.runWriteCommandAction(e.project, "Show Hidden Sections", null, {
            markers.reversed().forEach { deleteLine(document, it) }
        })
        folding.runBatchFoldingOperation { regions.forEach { it.isExpanded = true } }
        tell(editor, "CL: Revealed ${plural(markers.size, "hidden section")}")
    }
}
