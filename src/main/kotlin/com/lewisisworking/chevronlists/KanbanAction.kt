/**
 * KanbanAction.kt
 * IntelliJ bridge for Kanban.kt: a dialog with the file's items in Todo,
 * In Progress and Done columns. Double-click or Enter on a card jumps to it.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

class ShowKanbanAction : MarkdownEditorAction(
    "CL: Show Kanban",
    "Show this file's items as a board: Todo, In Progress (starred) and Done ([x])"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = ChevronListsSettings.getInstance().state.listPrefix
        val board  = kanbanBoard(editor.document.text.split("\n"), prefix)
        val name   = e.getData(CommonDataKeys.VIRTUAL_FILE)?.name ?: "Kanban"
        KanbanDialog(editor, board, name).show()
    }
}

private class KanbanDialog(private val editor: Editor, private val board: KanbanBoard, fileName: String) :
    DialogWrapper(editor.project, false) {

    init {
        title = "Kanban: $fileName"
        isModal = false
        setCancelButtonText("Close")
        init()
    }

    override fun createActions() = arrayOf(cancelAction)

    override fun createCenterPanel(): JComponent = JPanel(GridLayout(1, 3, JBUI.scale(8), 0)).apply {
        add(column("Todo", board.todo))
        add(column("In Progress", board.inProgress))
        add(column("Done", board.done))
        preferredSize = JBUI.size(780, 420)
    }

    private fun column(title: String, cards: List<KanbanCard>): JComponent {
        val list = JBList(cards).apply {
            cellRenderer = CardRenderer
            emptyText.text = "Nothing here"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { if (e.clickCount == 2) open(selectedValue) }
            })
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) { if (e.keyCode == KeyEvent.VK_ENTER) open(selectedValue) }
            })
        }
        return JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            add(JBLabel("$title (${cards.size})").apply { font = JBUI.Fonts.label().asBold() }, BorderLayout.NORTH)
            add(JBScrollPane(list), BorderLayout.CENTER)
        }
    }

    private fun open(card: KanbanCard?) {
        card ?: return
        editor.caretModel.moveToLogicalPosition(LogicalPosition(card.line, 0))
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER_UP)
        close(OK_EXIT_CODE)
    }

    private object CardRenderer : ColoredListCellRenderer<KanbanCard>() {
        override fun customizeCellRenderer(list: JList<out KanbanCard>, value: KanbanCard?, index: Int, selected: Boolean, hasFocus: Boolean) {
            value ?: return
            append(value.content)
            if (value.section.isNotEmpty()) append("  ${value.section}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}
