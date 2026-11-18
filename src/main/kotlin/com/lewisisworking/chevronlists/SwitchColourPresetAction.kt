/**
 * SwitchColourPresetAction.kt
 * IntelliJ bridge for ColourPresets.kt: a searchable popup of the 13 built-in
 * colour presets; choosing one saves it as the active preset so the annotator
 * paints chevron syntax with it. Ported from the VS Code extension's
 * CL: Colour Theme (switchColourPreset) command, which applies the preset and
 * remembers it in settings.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.StatusBar

class SwitchColourPresetAction : AnAction(
    "CL: Colour Theme",
    "Pick one of the 13 colour presets for chevron syntax highlighting",
    null
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file   = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && file != null && file.name.endsWith(".md")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor  = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val current = ChevronListsSettings.getInstance().state.colourPreset

        val popup = JBPopupFactory.getInstance().createListPopup(
            PresetPopupStep(current) { preset ->
                ChevronListsSettings.getInstance().state.colourPreset = preset.id
                DaemonCodeAnalyzer.getInstance(project).restart()
                StatusBar.Info.set("CL: Colour preset changed to \"${preset.id}\"", project)
            }
        )
        popup.showInBestPositionFor(editor)
    }

    private class PresetPopupStep(
        private val currentId: String,
        private val onPick: (ColourPreset) -> Unit
    ) : BaseListPopupStep<ColourPreset>("Colour Theme", COLOUR_PRESETS) {

        override fun getTextFor(value: ColourPreset): String =
            if (value.id == currentId) "${value.label} (active)" else value.label

        override fun onChosen(selected: ColourPreset?, finalChoice: Boolean): PopupStep<*>? {
            if (selected != null) onPick(selected)
            return FINAL_CHOICE
        }
    }
}
