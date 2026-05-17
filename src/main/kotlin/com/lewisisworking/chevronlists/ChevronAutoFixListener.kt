/**
 * ChevronAutoFixListener.kt
 * Application-level DocumentListener that watches all open documents. When a
 * markdown file's numbering becomes inconsistent (e.g. typing duplicates a
 * number or breaks a sequence), the listener debounces 250ms and then applies
 * the fix in a WriteCommandAction.
 *
 * The pure logic lives in `Diagnostics.kt::computeAutoFixEdits`. This class
 * is just the IntelliJ bridge.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.util.Alarm

@Service(Service.Level.APP)
class ChevronAutoFixListener : DocumentListener, Disposable {

    private val alarm    = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var applying = false

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(this, this)
    }

    override fun dispose() {}

    override fun documentChanged(event: DocumentEvent) {
        if (applying) return
        if (!ChevronListsSettings.getInstance().state.autoFixNumbering) return

        val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
        if (!file.name.endsWith(".md")) return

        val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return

        alarm.cancelAllRequests()
        alarm.addRequest({ runFix(event.document, project) }, DEBOUNCE_MS)
    }

    private fun runFix(document: Document, project: Project) {
        val lines = document.text.split("\n").mapIndexed { i, t -> AutoFixLine(t, i) }
        val edits = computeAutoFixEdits(lines)
        if (edits.isEmpty()) return

        applying = true
        try {
            WriteCommandAction.runWriteCommandAction(project) {
                // Apply edits bottom-up so earlier line numbers stay valid
                for (edit in edits.sortedByDescending { it.lineIndex }) {
                    val start = document.getLineStartOffset(edit.lineIndex)
                    val end   = document.getLineEndOffset(edit.lineIndex)
                    document.replaceString(start, end, edit.newText)
                }
            }
        } finally {
            applying = false
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 250

        /** Ensures the application-level listener service is constructed and active */
        fun ensureInitialised() {
            service<ChevronAutoFixListener>()
        }
    }
}