/**
 * ChevronTimerWidget.kt
 * Status bar text showing the running timers. It is hidden while none is running,
 * and clicking it stops one.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.Timer

private const val WIDGET_ID = "ChevronLists.Timers"
private const val TICK_MS = 1000

class ChevronTimerWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = WIDGET_ID
    override fun getDisplayName(): String = "Chevron Lists Timers"
    override fun createWidget(project: Project): StatusBarWidget = ChevronTimerWidget(project)
}

private class ChevronTimerWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    private var statusBar: StatusBar? = null
    private var shown = ""
    private val ticker = Timer(TICK_MS) { refresh() }

    private fun timers() = project.service<ChevronTimers>()

    private fun refresh() {
        val text = getText()
        if (text == shown) return
        shown = text
        statusBar?.updateWidget(WIDGET_ID)
    }

    override fun ID(): String = WIDGET_ID
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        ticker.start()
    }

    override fun dispose() {
        ticker.stop()
        statusBar = null
    }

    override fun getText(): String = timers().statusText()
    override fun getAlignment(): Float = Component.LEFT_ALIGNMENT
    override fun getTooltipText(): String = "CL: Running timers - click to stop one"

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        timers().stopAny()?.let { message -> StatusBar.Info.set(message, project) }
        refresh()
    }
}
