package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileMovesTest {

    @Test fun `a block runs through everything nested under the item`() {
        assertEquals(1..3, itemBlockRange(listOf("> S", ">> - a", ">>> - b", ">>>> - c", ">> - d"), 1, "-"))
    }

    @Test fun `an item with no children is a block of one`() {
        assertEquals(1..1, itemBlockRange(listOf("> S", ">>> - a", ">> - b"), 1, "-"))
    }

    @Test fun `a line that is not an item has no block`() {
        assertNull(itemBlockRange(listOf("> S"), 0, "-"))
    }

    @Test fun `a block goes after the section's items and before its blank lines`() {
        assertEquals(
            listOf("> D", ">> - d", ">> - new", ">>> - kid", "", "> E"),
            withBlockInSection(listOf("> D", ">> - d", "", "> E"), 0, listOf(">> - new", ">>> - kid")),
        )
    }

    @Test fun `a block goes straight under the header of an empty section`() {
        assertEquals(listOf("> D", ">> - new"), withBlockInSection(listOf("> D"), 0, listOf(">> - new")))
    }

    @Test fun `a section block leaves off its closing blank lines`() {
        assertEquals(listOf("> A", ">> - a"), sectionBlock(listOf("> A", ">> - a", "", "", "> B"), 0))
    }

    @Test fun `a copied section is separated from the file by one blank line`() {
        assertEquals(listOf("> Z", "", "> A", ">> - a", ""), withSectionAtEnd(listOf("> Z", "", ""), listOf("> A", ">> - a")))
        assertEquals(listOf("> Z", "", "> A", ""), withSectionAtEnd(listOf("> Z"), listOf("> A")))
    }

    @Test fun `a copied section starts an empty file`() {
        assertEquals(listOf("> A", ""), withSectionAtEnd(listOf(""), listOf("> A")))
    }
}
