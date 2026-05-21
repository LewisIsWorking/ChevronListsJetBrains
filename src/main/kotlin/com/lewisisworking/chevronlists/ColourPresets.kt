/**
 * ColourPresets.kt
 * Pure data: the 13 named colour presets ported from the Chevron Lists VS Code
 * extension. Each preset bundles co-ordinated foreground colours and bold flags
 * for the header chevron, item chevrons, and numbered-item digits. Markdown files
 * edited with the same preset in either editor render identically.
 *
 * No IntelliJ Platform imports - fully testable.
 */
package com.lewisisworking.chevronlists

/** Foreground colour (hex `#RRGGBB`, or null to inherit) plus optional bold flag */
data class TokenColour(val foreground: String? = null, val bold: Boolean = false)

/** A named colour preset bundling tokens for every supported chevron element */
data class ColourPreset(
    val id:          String,
    val label:       String,
    val description: String,
    val header:      TokenColour,
    val prefix:      TokenColour,
    val number:      TokenColour
)

/** Static list of all built-in colour presets, in dropdown order */
val COLOUR_PRESETS: List<ColourPreset> = listOf(
    ColourPreset("default",    "Default",
        "Violet headers - slate prefixes - lime numbers (matches the plugin icon)",
        TokenColour("#A855F7", true), TokenColour("#637880"), TokenColour("#84CC16")
    ),
    ColourPreset("classic",    "Classic",
        "Amber headers - grey prefixes - blue numbers (the original VS Code theme)",
        TokenColour("#E5C07B", true), TokenColour("#5C6370"), TokenColour("#61AFEF")
    ),
    ColourPreset("ocean",      "Ocean",
        "Teal headers - slate prefixes - cyan numbers",
        TokenColour("#56B6C2", true), TokenColour("#4B5263"), TokenColour("#2BBAC5")
    ),
    ColourPreset("forest",     "Forest",
        "Green headers - dark prefixes - lime numbers",
        TokenColour("#98C379", true), TokenColour("#3E5730"), TokenColour("#7CC26E")
    ),
    ColourPreset("sunset",     "Sunset",
        "Coral headers - muted orange prefixes - gold numbers",
        TokenColour("#E06C75", true), TokenColour("#6B4C3B"), TokenColour("#E5C07B")
    ),
    ColourPreset("monochrome", "Monochrome",
        "Bold white headers - grey prefixes - silver numbers",
        TokenColour("#FFFFFF", true), TokenColour("#5C6370"), TokenColour("#ABB2BF")
    ),
    ColourPreset("midnight",   "Midnight",
        "Purple headers - indigo prefixes - lavender numbers",
        TokenColour("#C792EA", true), TokenColour("#4A4080"), TokenColour("#A29BFE")
    ),
    ColourPreset("rose",       "Rose",
        "Pink headers - mauve prefixes - peach numbers",
        TokenColour("#F48FB1", true), TokenColour("#6D3B4F"), TokenColour("#FFAB91")
    ),
    ColourPreset("autumn",     "Autumn",
        "Orange headers - brown prefixes - red numbers",
        TokenColour("#FF9800", true), TokenColour("#5D3A1A"), TokenColour("#EF5350")
    ),
    ColourPreset("arctic",     "Arctic",
        "Ice blue headers - cool grey prefixes - white numbers",
        TokenColour("#89DDFF", true), TokenColour("#546E7A"), TokenColour("#ECEFF1")
    ),
    ColourPreset("neon",       "Neon",
        "Bright green headers - dark prefixes - cyan numbers",
        TokenColour("#00FF7F", true), TokenColour("#1A2A1A"), TokenColour("#00E5FF")
    ),
    ColourPreset("sepia",      "Sepia",
        "Warm tan headers - brown prefixes - gold numbers",
        TokenColour("#C9A96E", true), TokenColour("#7C5C3E"), TokenColour("#E8C97A")
    ),
    ColourPreset("custom",     "Custom",
        "Use per-scheme colours from Settings -> Editor -> Color Scheme -> Chevron Lists",
        TokenColour(), TokenColour(), TokenColour()
    )
)

/** Returns the preset matching the id, or the default preset if none found */
fun findPreset(id: String): ColourPreset =
    COLOUR_PRESETS.firstOrNull { it.id == id } ?: COLOUR_PRESETS.first()

/** Returns true if the preset is the "custom" escape-hatch (use per-scheme colours) */
fun isCustomPreset(id: String): Boolean = id == "custom"