/**
 * FocusDimActions.kt
 * Thin action over FocusDim.kt: dims every section except the caret's while
 * focus mode is on, following the caret as it moves. Toggling off removes
 * only this action's highlighters, never the annotator's.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.ColorUtil
import java.awt.Font

private object FocusDim {
    private data class Session(val listener: CaretListener, val dimmed: MutableList<RangeHighlighter>)

    private val sessions = mutableMapOf<Editor, Session>()

    fun isActive(editor: Editor) = sessions.containsKey(editor)

    fun toggle(editor: Editor) {
        if (isActive(editor)) stop(editor, true) else start(editor)
    }

    private fun start(editor: Editor) {
        if (editor.isDisposed) return
        val listener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = refresh(event.editor)
        }
        editor.caretModel.addCaretListener(listener)
        sessions[editor] = Session(listener, mutableListOf())
        refresh(editor)
        HintManager.getInstance().showInformationHint(editor, "CL: Focus mode on - other sections dimmed")
    }

    fun stop(editor: Editor, tell: Boolean) {
        val session = sessions.remove(editor) ?: return
        editor.caretModel.removeCaretListener(session.listener)
        if (!editor.isDisposed) {
            val markup = editor.markupModel
            session.dimmed.forEach { highlighter ->
                try { markup.removeHighlighter(highlighter) } catch (_: IllegalStateException) { }
            }
        }
        session.dimmed.clear()
        if (tell && !editor.isDisposed) {
            HintManager.getInstance().showInformationHint(editor, "CL: Focus mode off")
        }
    }

    private fun refresh(editor: Editor) {
        val session = sessions[editor] ?: return
        if (editor.isDisposed) { sessions.remove(editor); return }
        val markup = editor.markupModel
        session.dimmed.forEach { highlighter ->
            try { markup.removeHighlighter(highlighter) } catch (_: IllegalStateException) { }
        }
        session.dimmed.clear()
        val document = editor.document
        val lines    = document.text.split("\n")
        val caret    = document.getLineNumber(editor.caretModel.offset)
        val base     = editor.colorsScheme.defaultForeground
        val attrs    = TextAttributes(ColorUtil.toAlpha(base, 90), null, null, null, Font.PLAIN)
        for (range in dimmedRanges(lines, caret)) {
            val last = range.last.coerceAtMost(document.lineCount - 1)
            val first = range.first.coerceAtMost(last)
            session.dimmed += markup.addRangeHighlighter(
                document.getLineStartOffset(first), document.getLineEndOffset(last),
                HighlighterLayer.SELECTION - 1, attrs, HighlighterTargetArea.LINES_IN_RANGE
            )
        }
    }
}

class ToggleFocusDimAction : MarkdownEditorAction(
    "CL: Toggle Focus Mode (Dim)", "Dim every section except the caret's; follows the caret"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        FocusDim.toggle(editor)
    }
}
