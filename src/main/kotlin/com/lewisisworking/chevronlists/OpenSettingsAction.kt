/**
 * OpenSettingsAction.kt
 * Placeholder action that will eventually open the Chevron Lists settings panel.
 * For v0.1 this just shows an info dialog so the action infrastructure is wired up.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class OpenSettingsAction : AnAction("CL: Open Settings", "Open the Chevron Lists settings panel", null) {
    override fun actionPerformed(e: AnActionEvent) {
        Messages.showInfoMessage(
            e.project,
            "Chevron Lists settings panel coming in a future release. For now, all settings are managed through the IDE Settings dialog.",
            "Chevron Lists"
        )
    }
}