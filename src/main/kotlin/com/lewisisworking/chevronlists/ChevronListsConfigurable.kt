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
import javax.swing.JList

class ChevronListsConfigurable : BoundConfigurable("Chevron Lists") {

    private val state = ChevronListsSettings.getInstance().state

    /**
     * Internal stored values stay as "unordered"/"ordered" (consistent with the VS Code
     * extension's config schema), but the dropdown shows clearer human-readable labels.
     */
    private val listTypeValues = listOf("unordered", "ordered")
    // Subclasses SimpleListCellRenderer rather than calling SimpleListCellRenderer
    // .create(...). EVERY static create() overload is deprecated and marked for
    // removal -- both create(nullValue, getText) and the newer create(Customizer)
    // -- which the Plugin Verifier reports against 2026.2+. The class itself is
    // not deprecated, so overriding customize() is stable across the whole
    // declared range.
    //
    // The modern com.intellij.ui.dsl.listCellRenderer DSL is the other
    // replacement, but it postdates 2024.3, and this plugin's since-build is 243.
    // Subclassing is what works on every IDE we claim to support. It matters that
    // this stays current: with no until-build there is no version ceiling to stop
    // the plugin loading into the IDE that finally drops the removed API.
    private val listTypeRenderer = object : SimpleListCellRenderer<String?>() {
        override fun customize(
            list: JList<out String?>, value: String?,
            index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            text = when (value) {
                "ordered"   -> "Numbered list (>> 1.)"
                "unordered" -> "Bullet list (>> -)"
                else        -> value ?: ""
            }
        }
    }

    private val presetIds = COLOUR_PRESETS.map { it.id }
    private val presetRenderer = object : SimpleListCellRenderer<String?>() {
        override fun customize(
            list: JList<out String?>, value: String?,
            index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            text = value?.let { findPreset(it)?.label } ?: value ?: ""
        }
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