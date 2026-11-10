/**
 * JumpHistoryActions.kt
 * Remembers where header jumps started, on each document, and goes back to those places.
 * The positions are range markers, so they follow their text as the file changes.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.Key

private const val MAX_JUMPS = 10
private const val NO_JUMPS = "CL: No jump history for this file"
private val JUMPS = Key.create<MutableList<RangeMarker>>("ChevronLists.JumpHistory")

private fun liveJumps(document: Document): MutableList<RangeMarker> {
    val jumps = document.getUserData(JUMPS) ?: mutableListOf<RangeMarker>().also { document.putUserData(JUMPS, it) }
    jumps.removeAll { !it.isValid }
    return jumps
}

/** Records the caret position, before a jump moves it */
internal fun pushJump(editor: Editor) {
    val jumps  = liveJumps(editor.document)
    val offset = editor.caretModel.offset
    jumps += editor.document.createRangeMarker(offset, offset)
    if (jumps.size > MAX_JUMPS) jumps.removeAt(0).dispose()
}

/** Offsets of the recorded jumps, newest first */
internal fun jumpOffsets(document: Document): List<Int> = liveJumps(document).map { it.startOffset }.reversed()

private fun jumpPositions(editor: Editor): List<Pair<Int, Int>> = jumpOffsets(editor.document).map { offset ->
    val position = editor.offsetToLogicalPosition(offset)
    position.line to position.column
}

class JumpBackAction : MarkdownEditorAction("CL: Jump Back", "Go back to where the last header jump started") {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val marker = liveJumps(editor.document).removeLastOrNull()
            ?: return HintManager.getInstance().showInformationHint(editor, NO_JUMPS)
        editor.caretModel.moveToOffset(marker.startOffset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        marker.dispose()
    }
}

class ShowJumpHistoryAction : MarkdownEditorAction("CL: Show Jump History", "List where recent header jumps started") {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        showReport(editor, "Jump History", jumpHistoryEntries(jumpPositions(editor)), NO_JUMPS)
    }
}

class ShowRecentSectionsAction : MarkdownEditorAction("CL: Show Recent Sections", "List the sections recent header jumps left") {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val jumps  = jumpPositions(editor)
        if (jumps.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "CL: No navigation history yet - jump between headers to build it")
            return
        }
        showReport(editor, "Recent Sections", recentSectionEntries(documentLines(editor), jumps.map { it.first }), "CL: No section history found")
    }
}
