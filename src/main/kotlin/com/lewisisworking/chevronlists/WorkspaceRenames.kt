/**
 * WorkspaceRenames.kt
 * Pure logic for renaming a tag or a section across every markdown file of the
 * project. No IntelliJ Platform imports - fully testable with plain JUnit.
 * Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** One file's lines before and after a rename, and how many lines differ */
class FileChange(val path: String, val before: List<String>, val after: List<String>) {
    val changed: Int get() = before.indices.count { before[it] != after[it] }
}

private fun changesIn(files: List<ScannedFile>, rewrite: (ScannedFile) -> List<String>): List<FileChange> =
    files.mapNotNull { file -> rewrite(file).takeIf { it != file.lines }?.let { FileChange(file.path, file.lines, it) } }

/** Pure: [old] renamed to [new] on the items of every file; only the files that change */
fun workspaceTagRenames(files: List<ScannedFile>, listPrefix: String, old: String, new: String): List<FileChange> =
    changesIn(files) { renameTagInLines(it.lines, listPrefix, old, new).lines }

/**
 * Pure: the section at [header] of the file at [path] renamed to [newName], and
 * every `[[Old name]]` link in every file rewritten; only the files that change.
 * A section of the same name in another file keeps its header.
 */
fun workspaceSectionRenames(files: List<ScannedFile>, path: String, header: Int, newName: String): List<FileChange> {
    val source  = files.first { it.path == path }
    val name    = newName.trim()
    val oldLink = "[[${parseHeader(source.lines[header])!!.content.trim()}]]"
    return changesIn(files) { file ->
        if (file.path == path) renameSection(file.lines, header, name).lines
        else file.lines.map { it.replace(oldLink, "[[$name]]") }
    }
}
