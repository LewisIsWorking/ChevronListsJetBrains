/**
 * ChevronSnapshots.kt
 * The section snapshots of a project, kept in its workspace file like VS Code's
 * workspace state, and the one item snapshot, which lasts for the session.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@State(name = "ChevronListsSnapshots", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class ChevronSnapshots : PersistentStateComponent<ChevronSnapshots.Saved> {

    /** A bean so XmlSerializer can store it */
    class Snapshot(var name: String = "", var file: String = "", var content: String = "", var savedAt: String = "")

    class Saved(var snapshots: MutableList<Snapshot> = mutableListOf())

    private var saved = Saved()

    var itemSnapshot: String? = null

    override fun getState(): Saved = saved

    override fun loadState(state: Saved) { saved = state }

    fun forFile(file: String): List<Snapshot> = saved.snapshots.filter { it.file == file }

    fun add(snapshot: Snapshot) { saved.snapshots.add(snapshot) }
}
