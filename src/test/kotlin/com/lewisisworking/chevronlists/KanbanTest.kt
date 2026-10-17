/**
 * KanbanTest.kt
 * Plain JUnit 4 tests for kanbanBoard.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class KanbanTest {

    private val board = kanbanBoard(listOf(
        "intro text",          // 0
        "> Work",              // 1
        ">> - [x] ship it",    // 2
        ">> - ⭐ review PR",    // 3
        ">> 1. * write docs",  // 4
        ">> - plain task",     // 5
        ">>> - [ ] sub task",  // 6
        ">> - ",               // 7: empty
        "> Home",              // 8
        ">> - [X] ⭐ dishes",   // 9: done wins over the star
    ), "-")

    @Test fun `checked items are done, and done wins over a star`() =
        assertEquals(listOf(KanbanCard("ship it", "Work", 2), KanbanCard("dishes", "Home", 9)), board.done)

    @Test fun `starred items are in progress, the star left out`() =
        assertEquals(listOf(KanbanCard("review PR", "Work", 3), KanbanCard("write docs", "Work", 4)), board.inProgress)

    @Test fun `everything else is to do, nested items too, empty ones left out`() =
        assertEquals(listOf(KanbanCard("plain task", "Work", 5), KanbanCard("sub task", "Work", 6)), board.todo)

    @Test fun `a file with no items has an empty board`() =
        assertEquals(KanbanBoard(emptyList(), emptyList(), emptyList()), kanbanBoard(listOf("> Empty", "text"), "-"))
}
