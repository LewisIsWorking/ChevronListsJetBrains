/**
 * ChevronListsColorSettingsPage.kt
 * Registers Chevron Lists' TextAttributesKey constants under
 * Settings -> Editor -> Color Scheme -> Chevron Lists so users can customise
 * each colour to match their preference or colour scheme.
 *
 * A demo text snippet shows each token type highlighted live, so users can
 * see exactly which colour controls which kind of element.
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class ChevronListsColorSettingsPage : ColorSettingsPage {

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Header chevron (>)",           ChevronListsAnnotator.HEADER_KEY),
            AttributesDescriptor("Item chevrons (>>, >>>, ...)", ChevronListsAnnotator.CHEVRON_KEY),
            AttributesDescriptor("Tag (#urgent, #blocked)",      ChevronListsAnnotator.TAG_KEY),
            AttributesDescriptor("Priority - high (!!!)",        ChevronListsAnnotator.PRIORITY_HIGH_KEY),
            AttributesDescriptor("Priority - medium (!!)",       ChevronListsAnnotator.PRIORITY_MEDIUM_KEY),
            AttributesDescriptor("Priority - low (!)",           ChevronListsAnnotator.PRIORITY_LOW_KEY),
            AttributesDescriptor("Due date (@YYYY-MM-DD)",       ChevronListsAnnotator.DATE_KEY)
        )

        private val TAG_TO_DESCRIPTOR_MAP = mapOf(
            "header"    to ChevronListsAnnotator.HEADER_KEY,
            "chev"      to ChevronListsAnnotator.CHEVRON_KEY,
            "tag"       to ChevronListsAnnotator.TAG_KEY,
            "phigh"     to ChevronListsAnnotator.PRIORITY_HIGH_KEY,
            "pmed"      to ChevronListsAnnotator.PRIORITY_MEDIUM_KEY,
            "plow"      to ChevronListsAnnotator.PRIORITY_LOW_KEY,
            "date"      to ChevronListsAnnotator.DATE_KEY
        )

        private val DEMO_TEXT = """
            <header>></header> Daily Standup
            <chev>>></chev> 1. <phigh>!!!</phigh> Critical bug fix <tag>#urgent</tag> <date>@2026-04-22</date>
            <chev>>></chev> 2. <pmed>!!</pmed> Deploy to staging <tag>#devops</tag>
            <chev>>></chev> 3. <plow>!</plow> Update documentation <tag>#docs</tag>
            <chev>>></chev> - ⭐ Important: review pull requests
            <chev>>></chev> - 📌 Pinned reminder
            <chev>>>></chev> - Nested sub-item
        """.trimIndent()
    }

    override fun getDisplayName(): String                              = "Chevron Lists"
    override fun getIcon(): Icon?                                      = null
    override fun getHighlighter(): SyntaxHighlighter                   = PlainSyntaxHighlighter()
    override fun getDemoText(): String                                 = DEMO_TEXT
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor>         = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        TAG_TO_DESCRIPTOR_MAP
}