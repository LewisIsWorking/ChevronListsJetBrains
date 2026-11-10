/**
 * TimerActions.kt
 * Editor actions that start and stop the item, section and focus timers.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor

abstract class TimerAction(
    text: String,
    description: String,
    private val run: (ChevronTimers, Editor) -> String,
) : MarkdownEditorAction(text, description) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        HintManager.getInstance().showInformationHint(editor, run(project.service(), editor))
    }
}

class StartItemTimerAction : TimerAction(
    "CL: Start Item Timer", "Start timing the item at the caret", { timers, editor -> timers.startItem(editor) }
)

class StopItemTimerAction : TimerAction(
    "CL: Stop Item Timer", "Stop the item timer and stamp the time on the item", { timers, _ -> timers.stopItem() }
)

class StartSectionTimerAction : TimerAction(
    "CL: Start Section Timer", "Start timing the section at the caret", { timers, editor -> timers.startSection(editor) }
)

class StopSectionTimerAction : TimerAction(
    "CL: Stop Section Timer", "Stop the section timer and report how long it ran", { timers, _ -> timers.stopSection() }
)

class StartFocusTimerAction : TimerAction(
    "CL: Start Focus Timer", "Start a countdown of the length set in the settings", { timers, _ -> timers.startFocus() }
)

class StopFocusTimerAction : TimerAction(
    "CL: Stop Focus Timer", "Stop the focus timer", { timers, _ -> timers.stopFocus() }
)
