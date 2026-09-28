/**
 * ChevronFoldingBuilder.kt
 * IntelliJ bridge for SectionFolds.kt: adds a fold for every `> Header`
 * section of a markdown file, alongside the Markdown plugin's own folds. The
 * collapsed section shows its item count after the header.
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
        return sectionFolds(document.text.split("\n"), prefix).map { fold ->
            val range = TextRange(document.getLineEndOffset(fold.headerLine), document.getLineEndOffset(fold.lastLine))
            FoldingDescriptor(root.node, range, null, foldPlaceholder(fold))
        }.toTypedArray()
    }

    // The placeholder is set on each descriptor above; this is only a fallback
    override fun getPlaceholderText(node: ASTNode): String = " ..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
