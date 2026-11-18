/**
 * ItemCountBadgeActions.kt
 * Thin action over the badgesEnabled setting: flips the item count badge and
 * re-runs code analysis so the inlay hints refresh. Mirrors the VS Code
 * extension's CL: Toggle Item Count Badge command.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent

class ToggleItemCountBadgeAction : MarkdownEditorAction(
    "CL: Toggle Item Count Badge",
    "Show or hide the item count after each section header"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val state = ChevronListsSettings.getInstance().state
        state.badgesEnabled = !state.badgesEnabled
        DaemonCodeAnalyzer.getInstance(project).restart()
        NotificationGroupManager.getInstance().getNotificationGroup("Chevron Lists")
            .createNotification(
                "CL: Item count badges ${if (state.badgesEnabled) "enabled" else "disabled"}",
                NotificationType.INFORMATION
            )
            .notify(project)
    }
}
