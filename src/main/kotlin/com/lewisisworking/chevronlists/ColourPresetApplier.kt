/**
 * ColourPresetApplier.kt
 * Translates a (presetId, tokenType) pair into a concrete IntelliJ TextAttributes,
 * or null when the preset is "custom" (caller should fall back to the
 * TextAttributesKey so the per-scheme customisation from ColorSettingsPage applies).
 */
package com.lewisisworking.chevronlists

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

/** Which preset slot to look up - matches the fields on ColourPreset */
enum class PresetToken { HEADER, PREFIX, NUMBER }

/**
 * Pure (after hex parsing): given the active preset id and a token type, returns the
 * TextAttributes to enforce on the annotation. Returns null when the preset is
 * "custom" so the caller falls back to the scheme-aware TextAttributesKey.
 */
fun resolvePresetAttributes(presetId: String, token: PresetToken): TextAttributes? {
    if (isCustomPreset(presetId)) return null

    val preset    = findPreset(presetId)
    val tokenData = when (token) {
        PresetToken.HEADER -> preset.header
        PresetToken.PREFIX -> preset.prefix
        PresetToken.NUMBER -> preset.number
    }

    val fg = tokenData.foreground?.let(::parseHex) ?: return null
    val style = if (tokenData.bold) Font.BOLD else Font.PLAIN
    return TextAttributes(fg, null, null, null, style)
}

/** Pure: parses a "#RRGGBB" hex string. Returns null on any parsing failure. */
private fun parseHex(hex: String): Color? = try {
    Color.decode(hex)
} catch (e: NumberFormatException) {
    null
}