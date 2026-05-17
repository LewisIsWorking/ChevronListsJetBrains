/**
 * ChevronListsConfigurable.kt
 * UI panel for the Settings dialog. Appears under Tools -> Chevron Lists.
 * Uses IntelliJ's Kotlin UI DSL v2 for layout and binds directly to the
 * persistent State data class.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty

class ChevronListsConfigurable : BoundConfigurable("Chevron Lists") {

    private val state = ChevronListsSettings.getInstance().state

    private val listTypeOptions = listOf("unordered", "ordered")

    override fun createPanel(): DialogPanel = panel {
        group("List Behaviour") {
            row("List prefix:") {
                textField()
                    .bindText(state::listPrefix)
                    .comment("Character used after >> for bullet items. Default: '-'. " +
                             "Change to '*' for >> *, etc.")
            }
            row("Default new list type:") {
                comboBox(listTypeOptions)
                    .bindItem(state::defaultNewListType.toNullableProperty())
                    .comment("Inserted when pressing Enter on a `> Header` line. " +
                             "'unordered' inserts '>> - ', 'ordered' inserts '>> 1. '.")
            }
        }
    }
}