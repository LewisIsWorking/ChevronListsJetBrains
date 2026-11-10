/**
 * ChevronTimersTest.kt
 * Runs the item, section and focus timers in a headless IDE, with a clock the test moves.
 */
package com.lewisisworking.chevronlists

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ChevronTimersTest : BasePlatformTestCase() {

    private lateinit var timers: ChevronTimers
    private var now = 1_000_000L

    override fun setUp() {
        super.setUp()
        timers = project.service()
        timers.reset()
        timers.clock = { now }
    }

    override fun tearDown() {
        try {
            timers.reset()
            ChevronListsSettings.getInstance().state.focusTimerMinutes = 25
        } finally {
            super.tearDown()
        }
    }

    private fun text() = myFixture.editor.document.text
    private fun act(id: String) = myFixture.performEditorAction("ChevronLists.$id")
    private fun open(body: String) { myFixture.configureByText("notes.md", body) }
    private fun minutes(n: Int) { now += n * 60_000L }
    private fun edit(block: () -> Unit) = WriteCommandAction.runWriteCommandAction(project) { block() }

    fun `test stopping an item timer stamps the item`() {
        open("> A\n>> - wo<caret>rk\n")
        act("StartItemTimer")
        minutes(5)
        act("StopItemTimer")
        assertEquals("> A\n>> - work ~5m\n", text())
        assertEquals("", timers.statusText())
    }

    fun `test the stamp replaces an estimate`() {
        open("> A\n>> - wo<caret>rk ~2h\n")
        act("StartItemTimer")
        minutes(5)
        act("StopItemTimer")
        assertEquals("> A\n>> - work ~5m\n", text())
    }

    fun `test a numbered item is stamped`() {
        open("> A\n>> 1. wo<caret>rk\n")
        act("StartItemTimer")
        minutes(5)
        act("StopItemTimer")
        assertEquals("> A\n>> 1. work ~5m\n", text())
    }

    fun `test the stamp follows the item when lines are added above it`() {
        open("> A\n>> - wo<caret>rk\n>> - other\n")
        act("StartItemTimer")
        val document = myFixture.editor.document
        edit { document.insertString(0, "> New\n>> - x\n\n") }
        minutes(5)
        act("StopItemTimer")
        assertEquals("> New\n>> - x\n\n> A\n>> - work ~5m\n>> - other\n", text())
    }

    fun `test a timer does not start off an item`() {
        open("> A<caret>\n>> - work\n")
        assertEquals("CL: Place the caret on a chevron item to start a timer", timers.startItem(myFixture.editor))
        assertEquals("", timers.statusText())
    }

    fun `test a second item timer is refused and the first keeps running`() {
        open("> A\n>> - fir<caret>st\n>> - second\n")
        act("StartItemTimer")
        myFixture.editor.caretModel.moveToLogicalPosition(LogicalPosition(2, 3))
        assertEquals("CL: A timer is already running - stop it first with CL: Stop Item Timer", timers.startItem(myFixture.editor))
        minutes(5)
        act("StopItemTimer")
        assertEquals("> A\n>> - first ~5m\n>> - second\n", text())
    }

    fun `test a line that stopped being an item is left alone`() {
        open("> A\n>> - wo<caret>rk\n")
        act("StartItemTimer")
        val document = myFixture.editor.document
        val start = document.getLineStartOffset(1)
        edit { document.deleteString(start, start + 5) }
        minutes(5)
        assertEquals("CL: Timer stopped - 5m elapsed", timers.stopItem())
        assertEquals("> A\nwork\n", text())
    }

    fun `test a read only file reports that the item was not stamped`() {
        open("> A\n>> - wo<caret>rk\n")
        act("StartItemTimer")
        val document = myFixture.editor.document
        document.setReadOnly(true)
        try {
            minutes(5)
            assertEquals("CL: Timer stopped at 5m, but the item could not be stamped", timers.stopItem())
        } finally {
            document.setReadOnly(false)
        }
        assertEquals("> A\n>> - work\n", text())
    }

    fun `test stopping with no item timer says so`() {
        assertEquals("CL: No timer is running", timers.stopItem())
    }

    fun `test the item timer shows in the status text`() {
        open("> A\n>> - wo<caret>rk\n")
        act("StartItemTimer")
        minutes(5)
        assertEquals("5m \"work\"", timers.statusText())
    }

    fun `test a section timer reports how long it ran`() {
        open("> A\n>> - wo<caret>rk\n")
        act("StartSectionTimer")
        now += 65_000
        assertEquals("A: 1:05", timers.statusText())
        assertEquals("CL: Timer stopped - \"A\" ran for 1:05", timers.stopSection())
        assertEquals("", timers.statusText())
        assertEquals("> A\n>> - work\n", text())
    }

    fun `test the section timer action stops the timer`() {
        open("> A\n>> - wo<caret>rk\n")
        act("StartSectionTimer")
        act("StopSectionTimer")
        assertEquals("", timers.statusText())
    }

    fun `test a second section timer is refused`() {
        open("> A<caret>\n> B\n")
        act("StartSectionTimer")
        myFixture.editor.caretModel.moveToLogicalPosition(LogicalPosition(1, 0))
        assertEquals("CL: Timer already running for \"A\" - stop it first", timers.startSection(myFixture.editor))
    }

    fun `test no section at the caret starts nothing`() {
        open("pro<caret>se\n")
        assertEquals("CL: No section found at the caret", timers.startSection(myFixture.editor))
        assertEquals("", timers.statusText())
    }

    fun `test stopping with no section timer says so`() {
        assertEquals("CL: No timer is running", timers.stopSection())
    }

    fun `test the focus timer counts down from the setting`() {
        ChevronListsSettings.getInstance().state.focusTimerMinutes = 10
        assertEquals("CL: Focus timer started - 10 minutes", timers.startFocus())
        assertEquals("Focus 10:00", timers.statusText())
        assertEquals(1, timers.focusAlarmsPending())
        minutes(1)
        assertEquals("Focus 9:00", timers.statusText())
    }

    fun `test a second focus timer is refused`() {
        timers.startFocus()
        assertEquals("CL: Focus timer already running - stop it first", timers.startFocus())
        assertEquals(1, timers.focusAlarmsPending())
    }

    fun `test stopping the focus timer cancels its end`() {
        timers.startFocus()
        assertEquals("CL: Focus timer stopped", timers.stopFocus())
        assertEquals(0, timers.focusAlarmsPending())
        assertEquals("", timers.statusText())
    }

    fun `test stopping with no focus timer says so`() {
        assertEquals("CL: No focus timer running", timers.stopFocus())
    }

    fun `test the focus timer actions start and stop it`() {
        open("> A<caret>\n")
        act("StartFocusTimer")
        assertEquals("Focus 25:00", timers.statusText())
        act("StopFocusTimer")
        assertEquals("", timers.statusText())
    }

    fun `test the end of a focus timer sends a notification`() {
        val seen = mutableListOf<String>()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) { seen += notification.content }
        })
        timers.startFocus()
        timers.finishFocus()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        assertEquals(listOf("CL: Focus timer complete! Time for a break."), seen)
        assertEquals("", timers.statusText())
    }

    fun `test running timers share the status text and stop in order`() {
        open("> A\n>> - wo<caret>rk\n")
        act("StartItemTimer")
        act("StartSectionTimer")
        timers.startFocus()
        assertEquals("0s \"work\"  |  A: 0:00  |  Focus 25:00", timers.statusText())
        assertEquals("CL: Timer stopped - 0s stamped on item", timers.stopAny())
        assertEquals("CL: Timer stopped - \"A\" ran for 0:00", timers.stopAny())
        assertEquals("CL: Focus timer stopped", timers.stopAny())
        assertNull(timers.stopAny())
    }

    fun `test the status bar widget shows the status text`() {
        timers.startFocus()
        val widget = ChevronTimerWidgetFactory().createWidget(project)
        try {
            assertEquals("Focus 25:00", (widget.getPresentation() as StatusBarWidget.TextPresentation).getText())
        } finally {
            widget.dispose()
        }
    }
}
