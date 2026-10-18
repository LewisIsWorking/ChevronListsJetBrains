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
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.toNullableProperty
import javax.swing.JComponent
import javax.swing.JList

/** Comment line length, in the UI DSL's units; the default is 70 */
const val COMMENT_LINE_LENGTH = 45

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

    // The default comment line length ran past the right edge of a narrow Settings
    // pane and added a sideways scrollbar. Word-wrap mode did not help: it still
    // asks for the unwrapped width. A shorter fixed length does.
    private fun <T : JComponent> Cell<T>.wrappedComment(text: String): Cell<T> =
        comment(text, maxLineLength = COMMENT_LINE_LENGTH)

    override fun createPanel(): DialogPanel = panel {
        group("List Behaviour") {
            row("List prefix:") {
                textField()
                    .bindText(state::listPrefix)
                    .wrappedComment("Character used after >> for bullet items. Default: '-'. " +
                             "Change to '*' for >> *, etc.")
            }
            row("Default new list type:") {
                comboBox(listTypeValues, listTypeRenderer)
                    .bindItem(state::defaultNewListType.toNullableProperty())
                    .wrappedComment("Inserted when pressing Enter on a `> Header` line.")
            }
            row {
                checkBox("Auto-fix numbering as you type")
                    .bindSelected(state::autoFixNumbering)
                    .wrappedComment("Automatically renumber items when sequences break " +
                             "(e.g. two `>> 2.` items, or `>> 1.` followed by `>> 3.`). " +
                             "Independent per section and per chevron depth.")
            }
            row {
                checkBox("Paste several lines as separate items")
                    .bindSelected(state::pasteLinesAsItems)
                    .wrappedComment("Pasting multi-line text into a chevron item puts each line on " +
                             "its own item at the same depth. Numbered items keep counting " +
                             "(with auto-fix on, the items after them are renumbered).")
            }
        }
        group("Daily Notes") {
            row("Folder:") {
                textField()
                    .bindText(state::dailyNotesFolder)
                    .wrappedComment("Where CL: Open Daily Note keeps YYYY-MM-DD.md files. Relative to the " +
                             "project folder, or an absolute path. Blank: the project folder.")
            }
            row("Template:") {
                textArea()
                    .rows(4)
                    .bindText(state::dailyNoteTemplate)
                    .wrappedComment("Text of a new daily note. {{date}}, {{weekday}} and {{day}} are filled " +
                             "in. Blank: a dated > header with one empty item.")
            }
        }
        group("AI Assist") {
            row("Anthropic API key:") {
                passwordField()
                    .columns(40)
                    .bindText(AnthropicApiKey::stored)
                    .wrappedComment("Used by CL: Suggest Items, Summarise Section and Expand Item. Kept in the " +
                             "IDE's password safe, not in settings files. Blank: the ANTHROPIC_API_KEY " +
                             "environment variable.")
            }
        }
        group("Appearance") {
            row("Colour preset:") {
                comboBox(presetIds, presetRenderer)
                    .bindItem(state::colourPreset.toNullableProperty())
                    .wrappedComment("Co-ordinated colour palette for chevron syntax highlighting. " +
                             "Choose 'Custom' to use per-scheme colours from " +
                             "Settings → Editor → Color Scheme → Chevron Lists. " +
                             "Re-open the markdown file to see preset changes take effect.")
            }
        }
    }
}