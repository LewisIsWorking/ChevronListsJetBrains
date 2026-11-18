/**
 * TipOfDay.kt
 * IntelliJ bridge for the tips in Tips.kt: shows a rotating tip in a
 * notification on project open until dismissed, with Try It, Next Tip and
 * Don't Show Again buttons. CL: Show Tip of the Day shows one on demand and
 * re-enables them. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import com.intellij.ide.DataManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

private const val TIP_INDEX_KEY = "chevronLists.tipIndex"
private const val TIPS_DISABLED_KEY = "chevronLists.tipsDisabled"

/** Shows the current rotating tip unless tips are dismissed */
fun showTipOfDay(project: Project) {
    val properties = PropertiesComponent.getInstance()
    if (properties.getBoolean(TIPS_DISABLED_KEY, false)) return
    val index = properties.getInt(TIP_INDEX_KEY, 0) % TIPS.size
    properties.setValue(TIP_INDEX_KEY, (index + 1).toString())
    showTip(project, index)
}

/** Shows the tip at [index] with Try It, Next Tip and Don't Show Again */
private fun showTip(project: Project, index: Int) {
    val tip = TIPS[index % TIPS.size]
    val group = NotificationGroupManager.getInstance().getNotificationGroup("Chevron Lists")
    val notification = group.createNotification(tipMessage(tip), NotificationType.INFORMATION)
    if (tip.actionId != null) {
        notification.addAction(NotificationAction.createSimpleExpiring("Try It") {
            val action = ActionManager.getInstance().getAction(tip.actionId) ?: return@createSimpleExpiring
            // Runs in the focused editor's context, so editor actions act where the caret is
            DataManager.getInstance().getDataContextFromFocusAsync().onSuccess { dataContext ->
                val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext)
                action.actionPerformed(event)
            }
        })
    }
    notification.addAction(NotificationAction.createSimpleExpiring("Next Tip") {
        val (nextIndex, _) = nextTip(TIPS, index)
        PropertiesComponent.getInstance().setValue(TIP_INDEX_KEY, (nextIndex + 1).toString())
        showTip(project, nextIndex)
    })
    notification.addAction(NotificationAction.createSimpleExpiring("Don't Show Again") {
        PropertiesComponent.getInstance().setValue(TIPS_DISABLED_KEY, true)
    })
    notification.notify(project)
}

class TipOfDayStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        showTipOfDay(project)
    }
}

class ShowTipOfDayAction : AnAction(
    "CL: Show Tip of the Day",
    "Show a rotating tip, and show tips on project open again",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        PropertiesComponent.getInstance().setValue(TIPS_DISABLED_KEY, false)
        showTipOfDay(project)
    }
}
