/**
 * ChevronTagCompletionContributor.kt
 * Registers a completion contributor that suggests existing #tags when the
 * user types after a `#` character in a markdown file.
 *
 * Triggered automatically by IntelliJ's completion system. Press Ctrl+Space
 * after typing `#` (or wait for the auto-popup) to see the suggestions.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class ChevronTagCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            TagProvider()
        )
    }

    private class TagProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context:    ProcessingContext,
            result:     CompletionResultSet
        ) {
            val file = parameters.originalFile
            if (!file.name.endsWith(".md")) return

            val text   = file.text
            val offset = parameters.offset

            // Walk backwards from the caret to find a tag-prefix context.
            val tagStart = findTagPrefixStart(text, offset) ?: return
            val prefix   = text.substring(tagStart, offset)

            val resultSet = result.withPrefixMatcher(prefix)
            for (tag in extractAllTags(text)) {
                resultSet.addElement(
                    LookupElementBuilder.create(tag)
                        .withTypeText("Chevron tag")
                        .bold()
                )
            }
        }

        /**
         * Returns the offset of the `#` that starts the tag prefix at the caret,
         * or null if the caret is not in a tag-completion context. A tag context
         * requires the `#` to be preceded by whitespace or start-of-document.
         */
        private fun findTagPrefixStart(text: String, caret: Int): Int? {
            var i = caret - 1
            while (i >= 0 && isTagContinuation(text[i])) {
                i--
            }
            if (i < 0 || text[i] != '#') return null
            if (i > 0 && !text[i - 1].isWhitespace()) return null
            return i
        }

        private fun isTagContinuation(c: Char): Boolean =
            c.isLetterOrDigit() || c == '_' || c == '-'
    }
}