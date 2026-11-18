/**
 * ItemAgeInlayProvider.kt
 * IntelliJ bridge for itemAgeHint in ItemAgeHints.kt: shows the age of items
 * created 30 or more days ago as an inlay hint at the end of the item line.
 * Nothing is written to the file.
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

class ItemAgeInlayProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? =
        if (file.name.endsWith(".md")) Collector else null

    private object Collector : OwnBypassCollector {
        override fun collectHintsForFile(file: PsiFile, sink: InlayTreeSink) {
            val lines  = file.text.split("\n")
            val prefix = ChevronListsSettings.getInstance().state.listPrefix
            val today  = LocalDate.now()
            for (i in lines.indices) {
                val content = parseBullet(lines[i], prefix)?.content
                    ?: parseNumbered(lines[i])?.content
                    ?: continue
                if (content.isBlank()) continue
                val hint = itemAgeHint(content, today) ?: continue
                sink.addPresentation(endOfLine(i), hintFormat = HintFormat.default) { text(hint) }
            }
        }
    }
}
