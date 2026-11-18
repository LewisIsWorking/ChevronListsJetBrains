/**
 * ItemCountBadgeProvider.kt
 * IntelliJ bridge for badgeText in ItemCountBadge.kt: shows each section's
 * item count as an inlay hint at the end of its header. Nothing is written to
 * the file. Mirrors the VS Code extension's item count badge decorations.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.OwnBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class ItemCountBadgeProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? =
        if (file.name.endsWith(".md")) Collector else null

    private object Collector : OwnBypassCollector {
        override fun collectHintsForFile(file: PsiFile, sink: InlayTreeSink) {
            val state = ChevronListsSettings.getInstance().state
            if (!state.badgesEnabled) return
            val lines = file.text.split("\n")
            for (i in lines.indices) {
                if (!isHeader(lines[i])) continue
                val text = badgeText(lines, i, state.listPrefix)
                sink.addPresentation(endOfLine(i), hintFormat = HintFormat.default) { text(text) }
            }
        }
    }
}
