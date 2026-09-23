/**
 * SectionNavigationActions.kt
 * IntelliJ bridge for SectionNavigation.kt: moves the caret to the next or
 * previous `> Header`. Enabled only in markdown files, so the shared shortcut
 * falls through to the IDE's own action everywhere else.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType

abstract class ChevronSectionJumpAction(
    actionText:        String,
    actionDescription: String,
    private val down:  Boolean
) : AnAction(actionText, actionDescription, null) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val lines  = editor.document.text.split("\n")
        val line   = editor.caretModel.logicalPosition.line
        val target = (if (down) findHeaderBelow(lines, line) else findHeaderAbove(lines, line)) ?: return
        editor.caretModel.moveToLogicalPosition(LogicalPosition(target, 0))
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
}

class JumpToNextHeaderAction : ChevronSectionJumpAction(
    actionText        = "CL: Jump to Next Header",
    actionDescription = "Move the caret to the next > section header",
    down              = true
)

class JumpToPreviousHeaderAction : ChevronSectionJumpAction(
    actionText        = "CL: Jump to Previous Header",
    actionDescription = "Move the caret to the previous > section header",
    down              = false
)
