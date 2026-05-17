/**
 * ChevronListsAnnotator.kt
 * Provides syntax highlighting and warning underlines for chevron-list markdown
 * files. Runs once per file (when element is PsiFile) and annotates each line.
 */
package com.lewisisworking.chevronlists

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
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
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        if (!element.name.endsWith(".md")) return

        val text  = element.text
        val seenHeaders     = HashMap<String, Int>()
        val seenSubheadings = HashMap<String, Int>()
        var offset = 0
        var lineNumber = 0

        for (line in text.split("\n")) {
            annotateChevrons(line, offset, holder)
            checkDuplicateHeader(line, offset, lineNumber, seenHeaders, holder)
            checkDuplicateSubheading(line, offset, lineNumber, seenSubheadings, holder)
            offset += line.length + 1
            lineNumber++
        }
    }

    private fun annotateChevrons(line: String, lineStart: Int, holder: AnnotationHolder) {
        val match = Regex("""^(>+)""").find(line) ?: return
        val range = TextRange(lineStart, lineStart + match.value.length)
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range).textAttributes(if (match.value.length == 1) HEADER_KEY else CHEVRON_KEY).create()
    }

    private fun checkDuplicateHeader(line: String, lineStart: Int, lineNumber: Int,
                                      seen: HashMap<String, Int>, holder: AnnotationHolder) {
        val header = parseHeader(line) ?: return
        val key    = header.content.trim().lowercase()
        val first  = seen[key]
        if (first != null) {
            val msg = "Duplicate section name \"${header.content}\" (first at line ${first + 1})"
            holder.newAnnotation(HighlightSeverity.WARNING, msg)
                .range(TextRange(lineStart, lineStart + line.length)).create()
        } else {
            seen[key] = lineNumber
        }
    }

    private fun checkDuplicateSubheading(line: String, lineStart: Int, lineNumber: Int,
                                          seen: HashMap<String, Int>, holder: AnnotationHolder) {
        val sub = parseSubheading(line) ?: return
        val key   = sub.content.lowercase()
        val first = seen[key]
        if (first != null) {
            val msg = "Duplicate subheading \"${sub.content}\" (first at line ${first + 1})"
            holder.newAnnotation(HighlightSeverity.WARNING, msg)
                .range(TextRange(lineStart, lineStart + line.length)).create()
        } else {
            seen[key] = lineNumber
        }
    }
}