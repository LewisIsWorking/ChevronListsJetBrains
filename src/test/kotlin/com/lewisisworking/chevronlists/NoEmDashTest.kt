/**
 * NoEmDashTest.kt
 * Keeps the repo free of em dashes (Lewis, 2026-09-21: none in any repo - code,
 * comments, docs or user-facing text). Escapes count too, since they still print
 * one. Write " - ", a comma, a colon or a full stop instead.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class NoEmDashTest {

    // Built in pieces so this file does not contain what it searches for
    private val forbidden = listOf(
        0x2014.toChar().toString(),
        "&" + "mdash;",
        "\\" + "u2014",
    )

    private val skippedDirs = setOf(".git", ".gradle", ".idea", ".kotlin", ".intellijPlatform", "build")
    private val textExtensions = setOf("kt", "kts", "java", "md", "xml", "yml", "yaml", "properties", "txt", "json", "html")

    @Test fun `no em dashes anywhere in the repo`() {
        val root = File(".").canonicalFile
        val offenders = root.walkTopDown()
            .onEnter { it.name !in skippedDirs }
            .filter { it.isFile && it.extension in textExtensions }
            .mapNotNull { file ->
                val hits = file.readLines().withIndex().filter { (_, line) -> forbidden.any { it in line } }
                if (hits.isEmpty()) null
                else "${file.relativeTo(root).path}: lines ${hits.joinToString { (it.index + 1).toString() }}"
            }
            .toList()
        assertEquals("Em dashes found (use \" - \", a comma, a colon or a full stop):", emptyList<String>(), offenders)
    }

    // A guard that scans nothing would pass for ever
    @Test fun `the scan actually reaches the source tree`() {
        val root = File(".").canonicalFile
        assertTrue(File(root, "src/main/kotlin/com/lewisisworking/chevronlists/Patterns.kt").isFile)
        assertTrue(File(root, "CHANGELOG.md").isFile)
    }
}
