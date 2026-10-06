/**
 * ChevronFoldingBuilder.kt
 * IntelliJ bridge for SectionFolds.kt: adds a fold for every `> Header`
 * section of a markdown file, alongside the Markdown plugin's own folds. The
 * collapsed section shows its header and item count.
 *
 * A section is also a Markdown block quote, which the Markdown plugin folds
 * over the same range. IntelliJ keeps the first fold it is given for a range,
 * so plugin.xml registers this builder with order="first".
 */
package com.lewisisworking.chevronlists

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class ChevronFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val lines  = document.text.split("\n")
        return sectionFolds(lines, prefix).map { fold ->
            val range = TextRange(document.getLineStartOffset(fold.headerLine), document.getLineEndOffset(fold.lastLine))
            FoldingDescriptor(root.node, range, null, foldPlaceholder(lines[fold.headerLine], fold))
        }.toTypedArray()
    }

    // The placeholder is set on each descriptor above; this is only a fallback
    override fun getPlaceholderText(node: ASTNode): String = " ..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
