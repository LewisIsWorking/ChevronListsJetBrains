/**
 * ChevronListsAnnotator.kt
 * Bridges pure logic (Patterns.kt, Diagnostics.kt) to IntelliJ Platform.
 * Performs syntax highlighting per line and creates warning annotations
 * from collectIssues() results.
 */
package com.lewisisworking.chevronlists

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ChevronListsAnnotator : Annotator {
    companion object {
        val CHEVRON_KEY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "CHEVRON_LISTS_CHEVRON", DefaultLanguageHighlighterColors.KEYWORD
        )
        val HEADER_KEY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "CHEVRON_LISTS_HEADER", DefaultLanguageHighlighterColors.MARKUP_TAG
        )
        private val CHEVRON_PREFIX = Regex("""^(>+)""")
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        if (!element.name.endsWith(".md")) return

        val text       = element.text
        val lines      = text.split("\n")
        val offsets    = computeLineOffsets(lines)

        annotateSyntax(lines, offsets, holder)
        annotateDiagnostics(lines, offsets, holder)
    }

    /** Highlights the chevron prefix on each line (single `>` as HEADER, `>>` or deeper as CHEVRON) */
    private fun annotateSyntax(lines: List<String>, offsets: IntArray, holder: AnnotationHolder) {
        for ((i, line) in lines.withIndex()) {
            val m = CHEVRON_PREFIX.find(line) ?: continue
            val range = TextRange(offsets[i], offsets[i] + m.value.length)
            val key   = if (m.value.length == 1) HEADER_KEY else CHEVRON_KEY
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range).textAttributes(key).create()
        }
    }

    /** Creates warning annotations for every issue returned by the pure collectIssues() */
    private fun annotateDiagnostics(lines: List<String>, offsets: IntArray, holder: AnnotationHolder) {
        for (issue in collectIssues(lines, "-")) {
            val line  = lines[issue.line]
            val start = offsets[issue.line]
            holder.newAnnotation(HighlightSeverity.WARNING, issue.message)
                .range(TextRange(start, start + line.length))
                .create()
        }
    }

    /** Pre-computes cumulative character offsets for each line so annotations can range cleanly */
    private fun computeLineOffsets(lines: List<String>): IntArray {
        val offsets = IntArray(lines.size)
        var cursor  = 0
        for ((i, line) in lines.withIndex()) {
            offsets[i] = cursor
            cursor += line.length + 1
        }
        return offsets
    }
}