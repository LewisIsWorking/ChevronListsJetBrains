/**
 * ItemTextTransformsTest.kt
 * Plain JUnit 4 tests for splitItem, joinItem and the item text transforms.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemTextTransformsTest {

    private val upper: (String) -> String = { changeItemWords(it) { s -> s.uppercase() } }

    @Test fun `splitItem reads every part, in any lead order`() =
        assertEquals(
            ItemParts("[X]", "!", "*", "{red}", "buy milk #shop", 3, "see https://x.com"),
            splitItem("{red} ! [X] * buy milk #shop +3 // see https://x.com")
        )

    @Test fun `joinItem writes the parts in parser order`() =
        assertEquals(
            "[x] ! * {red} buy milk #shop +3 // see https://x.com",
            joinItem(splitItem("{red} ! [X] * buy milk #shop +3 // see https://x.com"))
        )

    @Test fun `a plus inside a word is not a vote`() =
        assertNull(splitItem("learn C++11").vote)

    @Test fun `a vote too large to count stays in the words`() {
        val parts = splitItem("big +99999999999")
        assertNull(parts.vote)
        assertEquals("big +99999999999", parts.body)
    }

    @Test fun `uppercase leaves markers, vote and comment alone`() =
        assertEquals("[x] !! {red} BUY MILK +3 // see note", upper("[x] !! {red} buy milk +3 // see note"))

    @Test fun `uppercase leaves references alone`() =
        assertEquals(
            "ASK @Lewis ABOUT #work IN [[Two Words]] AT https://x.com/a NOW",
            upper("ask @Lewis about #work in [[Two Words]] at https://x.com/a now")
        )

    @Test fun `title case does not capitalise after an apostrophe`() =
        assertEquals("Don't Stop, It's Mine", toTitleCase("don't stop, it's mine"))

    @Test fun `title case handles accents and hyphens`() {
        assertEquals("Élan Vital", toTitleCase("élan vital"))
        assertEquals("Café Au Lait", toTitleCase("café au lait"))
        assertEquals("Well-Known Fact", toTitleCase("well-known fact"))
    }

    @Test fun `strikethrough wraps only the words and toggles off`() {
        assertEquals("[x] !! ~~done~~ +2 // note", toggleStrikethrough("[x] !! done +2 // note"))
        assertEquals("[x] !! done +2 // note", toggleStrikethrough("[x] !! ~~done~~ +2 // note"))
    }

    @Test fun `strikethrough leaves an item with no words alone`() =
        assertEquals("[x]", toggleStrikethrough("[x]"))

    @Test fun `remove strikethrough only touches struck words`() {
        assertEquals("[ ] old", removeStrikethrough("[ ] ~~old~~"))
        assertEquals("[ ] not struck", removeStrikethrough("[ ] not struck"))
    }

    @Test fun `a line keeps its chevrons, bullet and number`() {
        assertEquals(">>> * GO", transformItemLine(">>> * go", "*") { it.uppercase() })
        assertEquals(">> 4. GO", transformItemLine(">> 4. go", "-") { it.uppercase() })
    }

    @Test fun `only items are changed`() =
        assertNull(transformItemLine("> Header", "-") { it.uppercase() })
}
