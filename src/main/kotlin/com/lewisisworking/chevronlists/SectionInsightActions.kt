/**
 * SectionInsightActions.kt
 * IntelliJ bridge for SectionInsights.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import java.time.LocalDate

private const val NO_SECTION = "CL: No section found at cursor"

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun AnActionEvent.editor(): Editor? = getData(CommonDataKeys.EDITOR)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

private fun hint(editor: Editor, message: String) = HintManager.getInstance().showInformationHint(editor, message)

class ShowSectionWeightsAction : MarkdownEditorAction(
    "CL: Show Section Weights", "Rank the sections by items, priorities, votes and tags"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Sections by Weight", sectionWeights(documentLines(editor), listPrefix), "CL: No sections found")
    }
}

class ShowCompletionStreakAction : MarkdownEditorAction(
    "CL: Show Completion Streak", "List the sections with every checkbox done"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Completed Sections", completedSections(documentLines(editor), listPrefix), "CL: No fully-completed sections found")
    }
}

class SectionHealthCheckAction : MarkdownEditorAction(
    "CL: Section Health Check", "List the empty, very long and repeated items of this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        val line   = caretLine(editor)
        val issues = sectionHealthIssues(lines, line, listPrefix)
        if (issues == null) { hint(editor, NO_SECTION); return }
        val name = headerName(lines, headerAtOrAbove(lines, line)!!)
        showReport(editor, "${plural(issues.size, "issue")} in \"$name\"", issues, "CL: \"$name\" - no health issues found ✅")
    }
}

class PinSectionToTopAction : MarkdownEditorAction(
    "CL: Pin Section to Top", "Move this section above every other section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val before = documentLines(editor)
        val line   = caretLine(editor)
        if (headerAtOrAbove(before, line) == null) { hint(editor, NO_SECTION); return }
        val after = pinSectionToTop(before, line)
        if (after == null) { hint(editor, "CL: Section is already at the top"); return }
        val first = before.indexOfFirst { isHeader(it) }
        WriteCommandAction.runWriteCommandAction(e.project) {
            replaceChangedLines(editor.document, before, after)
            editor.caretModel.moveToLogicalPosition(LogicalPosition(first, 0))
        }
    }
}

class FindSimilarSectionsAction : MarkdownEditorAction(
    "CL: Find Similar Sections", "List the pairs of sections with nearly the same name"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val lines  = documentLines(editor)
        val empty  = if (lines.count { isHeader(it) } < 2) "CL: Need at least 2 sections to compare" else "CL: No similar section names found"
        showReport(editor, "Similar Sections", similarSections(lines), empty)
    }
}

class ShowProgressReportAction : MarkdownEditorAction(
    "CL: Show Progress Report", "List each section with its items, done count, flags, overdue items and word goal"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        showReport(editor, "Progress Report", progressReport(documentLines(editor), listPrefix, LocalDate.now().toString()), "CL: No sections found")
    }
}

class EvaluateExpressionAction : MarkdownEditorAction(
    "CL: Evaluate Expression in Item", "Replace the first =expression in the item at the caret with its value"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor() ?: return
        val line   = caretLine(editor)
        val before = documentLines(editor)
        if (itemContent(before[line], listPrefix) == null) { hint(editor, "CL: Place cursor on a chevron item"); return }
        val result = evaluateItemExpression(before[line], listPrefix)
        if (result == null) { hint(editor, "CL: No =expression found in item (e.g. =2+2)"); return }
        WriteCommandAction.runWriteCommandAction(e.project) {
            replaceChangedLines(editor.document, before, before.toMutableList().also { it[line] = result.line })
        }
        hint(editor, "CL: ${result.expression} = ${result.result}")
    }
}
