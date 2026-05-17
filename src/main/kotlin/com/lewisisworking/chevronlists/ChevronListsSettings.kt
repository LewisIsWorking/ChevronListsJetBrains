/**
 * ChevronListsSettings.kt
 * Application-level persistent settings for Chevron Lists. Stored in
 * `chevronLists.xml` under the IDE config directory and shared across all
 * projects. Mirrors a subset of the VS Code `ChevronConfig` interface.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name     = "ChevronListsSettings",
    storages = [Storage("chevronLists.xml")]
)
@Service(Service.Level.APP)
class ChevronListsSettings : PersistentStateComponent<ChevronListsSettings.State> {

    /** Mutable data class - must be a `var` data class for XmlSerializer to round-trip values */
    data class State(
        var listPrefix:         String  = "-",
        var defaultNewListType: String  = "unordered",
        var autoFixNumbering:   Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    companion object {
        /** Single shared instance retrieved through the IntelliJ service container */
        fun getInstance(): ChevronListsSettings = service()
    }
}