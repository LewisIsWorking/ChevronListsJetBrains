/**
 * SectionSummaryInlayProvider.kt
 * IntelliJ bridge for sectionHeaderHint in SectionSummaryHints.kt: shows each
 * section's item/done/tag/urgent/old totals as an inlay hint at the end of its
 * `> Header`. Nothing is written to the file.
 *
 * An inlay rather than an EditorLinePainter: line painters are not drawn in a
 * soft-wrapped editor, and markdown files are soft-wrapped by default.
 * Enabled or disabled in Settings, Editor, Inlay Hints, like the set totals.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.OwnBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import java.time.LocalDate

class SectionSummaryInlayProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? =
        if (file.name.endsWith(".md")) Collector else null

    private object Collector : OwnBypassCollector {
        override fun collectHintsForFile(file: PsiFile, sink: InlayTreeSink) {
            val lines  = file.text.split("\n")
            val prefix = ChevronListsSettings.getInstance().state.listPrefix
            val today  = LocalDate.now()
            for (i in lines.indices) {
                val hint = sectionHeaderHint(lines, i, prefix, today) ?: continue
                sink.addPresentation(endOfLine(i), hintFormat = HintFormat.default) { text(hint) }
            }
        }
    }
}
