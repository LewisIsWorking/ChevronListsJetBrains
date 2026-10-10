/**
 * SetTotalsInlayProvider.kt
 * IntelliJ bridge for setSummary in Sets.kt: shows a set's totals as an
 * inlay hint at the end of its `#set` heading. Nothing is written to the file.
 *
 * An inlay rather than an EditorLinePainter: line painters are not drawn in a
 * soft-wrapped editor, and markdown files are soft-wrapped by default.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hints.declarative.EndOfLinePosition
import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.OwnBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class SetTotalsInlayProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? =
        if (file.name.endsWith(".md")) Collector else null

    private object Collector : OwnBypassCollector {
        override fun collectHintsForFile(file: PsiFile, sink: InlayTreeSink) {
            val lines  = file.text.split("\n")
            val prefix = ChevronListsSettings.getInstance().state.listPrefix
            for (set in findSets(lines)) {
                val summary = setSummary(lines, set.headingLine, prefix) ?: continue
                sink.addPresentation(EndOfLinePosition(set.headingLine), hintFormat = HintFormat.default) { text(summary) }
            }
        }
    }
}
