/**
 * MarkerToggleActions.kt
 * Concrete marker-toggle actions. Each is a one-line subclass of
 * ChevronMarkerToggleAction that supplies its specific emoji marker.
 *
 * Markers match the VS Code Chevron Lists extension convention so a file
 * edited in either editor renders identically in the other.
 */
package com.lewisisworking.chevronlists

class ToggleStarAction : ChevronMarkerToggleAction(
    marker            = "⭐",
    actionText        = "CL: Toggle Star",
    actionDescription = "Toggle the ⭐ marker on the current chevron item"
)

class TogglePinAction : ChevronMarkerToggleAction(
    marker            = "📌",
    actionText        = "CL: Toggle Pin",
    actionDescription = "Toggle the 📌 marker on the current chevron item"
)

class ToggleFlagAction : ChevronMarkerToggleAction(
    marker            = "🚩",
    actionText        = "CL: Toggle Flag",
    actionDescription = "Toggle the 🚩 marker on the current chevron item"
)

class ToggleNoteAction : ChevronMarkerToggleAction(
    marker            = "📝",
    actionText        = "CL: Toggle Note",
    actionDescription = "Toggle the 📝 marker on the current chevron item"
)