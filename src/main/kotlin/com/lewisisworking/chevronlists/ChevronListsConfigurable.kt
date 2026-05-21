/**
 * ChevronListsConfigurable.kt
 * UI panel for the Settings dialog. Appears under Tools -> Chevron Lists.
 * Uses IntelliJ's Kotlin UI DSL v2 for layout and binds directly to the
 * persistent State data class.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty

class ChevronListsConfigurable : BoundConfigurable("Chevron Lists") {

    private val state = ChevronListsSettings.getInstance().state

    /**
     * Internal stored values stay as "unordered"/"ordered" (consistent with the VS Code
     * extension's config schema), but the dropdown shows clearer human-readable labels.
     */
    private val listTypeValues = listOf("unordered", "ordered")
    private val listTypeRenderer = SimpleListCellRenderer.create<String?>("") {
        when (it) {
            "ordered"   -> "Numbered list (>> 1.)"
            "unordered" -> "Bullet list (>> -)"
            else        -> it ?: ""
        }
    }

    private val presetIds = COLOUR_PRESETS.map { it.id }
    private val presetRenderer = SimpleListCellRenderer.create<String?>("") { id ->
        val preset = id?.let { findPreset(it) }
        preset?.label ?: id ?: ""
    }

    override fun createPanel(): DialogPanel = panel {
        group("List Behaviour") {
            row("List prefix:") {
                textField()
                    .bindText(state::listPrefix)
                    .comment("Character used after >> for bullet items. Default: '-'. " +
                             "Change to '*' for >> *, etc.")
            }
            row("Default new list type:") {
                comboBox(listTypeValues, listTypeRenderer)
                    .bindItem(state::defaultNewListType.toNullableProperty())
                    .comment("Inserted when pressing Enter on a `> Header` line.")
            }
            row {
                checkBox("Auto-fix numbering as you type")
                    .bindSelected(state::autoFixNumbering)
                    .comment("Automatically renumber items when sequences break " +
                             "(e.g. two `>> 2.` items, or `>> 1.` followed by `>> 3.`). " +
                             "Independent per section and per chevron depth.")
            }
        }
        group("Appearance") {
            row("Colour preset:") {
                comboBox(presetIds, presetRenderer)
                    .bindItem(state::colourPreset.toNullableProperty())
                    .comment("Co-ordinated colour palette for chevron syntax highlighting. " +
                             "Choose 'Custom' to use per-scheme colours from " +
                             "Settings → Editor → Color Scheme → Chevron Lists. " +
                             "Re-open the markdown file to see preset changes take effect.")
            }
        }
    }
}