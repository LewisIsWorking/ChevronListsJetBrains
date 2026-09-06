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
import java.lang.ref.WeakReference

@Service(Service.Level.APP)
class ChevronAutoFixListener : DocumentListener, Disposable {

    private val alarm    = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var applying = false

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(this, this)
    }

    override fun dispose() {
        // Drop any request still pending so it cannot fire after teardown
        alarm.cancelAllRequests()
    }

    override fun documentChanged(event: DocumentEvent) {
        if (applying) return
        if (!ChevronListsSettings.getInstance().state.autoFixNumbering) return

        val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
        if (!file.name.endsWith(".md")) return

        val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return

        scheduleFix(event.document, project)
    }

    /**
     * Queues the debounced fix.
     *
     * The pending request is held by an application-level Alarm, which outlives
     * any single project. Capturing the Document and Project strongly meant a
     * project closed inside the debounce window stayed reachable from the Alarm
     * until the request fired, and a leaked Project pins its whole PSI and index.
     * Weak references let it be collected, and the disposed check stops a
     * late-firing request from writing into a project that is closing.
     */
    private fun scheduleFix(document: Document, project: Project) {
        val documentRef = WeakReference(document)
        val projectRef  = WeakReference(project)

        alarm.cancelAllRequests()
        alarm.addRequest({
            val doc  = documentRef.get()  ?: return@addRequest
            val proj = projectRef.get()   ?: return@addRequest
            if (proj.isDisposed) return@addRequest
            runFix(doc, proj)
        }, DEBOUNCE_MS)
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