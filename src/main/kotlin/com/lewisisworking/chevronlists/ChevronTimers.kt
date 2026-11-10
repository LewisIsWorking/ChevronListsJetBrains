/**
 * ChevronTimers.kt
 * Holds the running item, section and focus timers of a project. Each kind runs one
 * at a time. Every start and stop returns the message to show.
 */
package com.lewisisworking.chevronlists

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.util.Alarm

@Service(Service.Level.PROJECT)
class ChevronTimers(private val project: Project) : Disposable {

    private class ItemTimer(val startMs: Long, val marker: RangeMarker, val label: String)
    private class SectionTimer(val startMs: Long, val name: String)

    internal var clock: () -> Long = { System.currentTimeMillis() }

    private var item: ItemTimer? = null
    private var section: SectionTimer? = null
    private var focusEnd: Long? = null
    private val focusAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private fun listPrefix() = ChevronListsSettings.getInstance().state.listPrefix

    fun startItem(editor: Editor): String {
        if (item != null) return "CL: A timer is already running - stop it first with CL: Stop Item Timer"
        val document = editor.document
        val line     = editor.caretModel.logicalPosition.line
        val start    = document.getLineStartOffset(line)
        val end      = document.getLineEndOffset(line)
        val text     = document.getText(TextRange(start, end))
        val content  = parseBullet(text, listPrefix())?.content ?: parseNumbered(text)?.content
            ?: return "CL: Place the caret on a chevron item to start a timer"
        item = ItemTimer(clock(), document.createRangeMarker(start, end), timerLabel(content))
        return "CL: Item timer started - click the status bar to stop"
    }

    fun stopItem(): String {
        val timer   = item ?: return "CL: No timer is running"
        item = null
        val elapsed = elapsedStamp(clock() - timer.startMs)
        val marker  = timer.marker
        val plain   = "CL: Timer stopped - $elapsed elapsed"
        if (!marker.isValid) return plain
        val document = marker.document
        val line     = document.getLineNumber(marker.startOffset)
        marker.dispose()
        val start   = document.getLineStartOffset(line)
        val end     = document.getLineEndOffset(line)
        val stamped = withElapsedStamp(document.getText(TextRange(start, end)), listPrefix(), elapsed) ?: return plain
        if (!document.isWritable) return "CL: Timer stopped at $elapsed, but the item could not be stamped"
        WriteCommandAction.runWriteCommandAction(project, "Stop Item Timer", null, {
            document.replaceString(start, end, stamped)
        })
        return "CL: Timer stopped - $elapsed stamped on item"
    }

    fun startSection(editor: Editor): String {
        section?.let { return "CL: Timer already running for \"${it.name}\" - stop it first" }
        val lines  = documentLines(editor)
        val header = headerAtOrAbove(lines, editor.caretModel.logicalPosition.line)
            ?: return "CL: No section found at the caret"
        val name   = plainSectionName(lines, header)
        section = SectionTimer(clock(), name)
        return "CL: Timer started for \"$name\""
    }

    fun stopSection(): String {
        val timer = section ?: return "CL: No timer is running"
        section = null
        return "CL: Timer stopped - \"${timer.name}\" ran for ${clockTime(clock() - timer.startMs)}"
    }

    fun startFocus(): String {
        if (focusEnd != null) return "CL: Focus timer already running - stop it first"
        val minutes = ChevronListsSettings.getInstance().state.focusTimerMinutes.coerceAtLeast(1)
        val length  = minutes * 60_000L
        focusEnd = clock() + length
        focusAlarm.addRequest({ finishFocus() }, length)
        return "CL: Focus timer started - ${plural(minutes, "minute")}"
    }

    fun stopFocus(): String {
        focusEnd ?: return "CL: No focus timer running"
        focusEnd = null
        focusAlarm.cancelAllRequests()
        return "CL: Focus timer stopped"
    }

    internal fun finishFocus() {
        focusEnd ?: return
        focusEnd = null
        focusAlarm.cancelAllRequests()
        NotificationGroupManager.getInstance().getNotificationGroup("Chevron Lists")
            .createNotification("CL: Focus timer complete! Time for a break.", NotificationType.INFORMATION)
            .notify(project)
    }

    internal fun focusAlarmsPending(): Int = focusAlarm.activeRequestCount

    /** Stops one running timer, the item timer first. Null if none is running */
    fun stopAny(): String? = when {
        item != null     -> stopItem()
        section != null  -> stopSection()
        focusEnd != null -> stopFocus()
        else             -> null
    }

    /** What the status bar shows; empty when no timer is running */
    fun statusText(): String {
        val now = clock()
        return listOfNotNull(
            item?.let { "${elapsedStamp(now - it.startMs)} \"${it.label}\"" },
            section?.let { "${it.name}: ${clockTime(now - it.startMs)}" },
            focusEnd?.let { "Focus ${clockTime((it - now).coerceAtLeast(0))}" },
        ).joinToString("  |  ")
    }

    /** Clears every timer, for tests: the service outlives each test on the shared project */
    internal fun reset() {
        item?.marker?.dispose()
        item = null
        section = null
        focusEnd = null
        focusAlarm.cancelAllRequests()
        clock = { System.currentTimeMillis() }
    }

    override fun dispose() = Unit
}
