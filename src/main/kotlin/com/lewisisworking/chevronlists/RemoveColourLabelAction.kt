/**
 * RemoveColourLabelAction.kt
 * Removes any {red}/{green}/{blue}/{yellow}/{orange}/{purple} label from the
 * current chevron item, or from every chevron item in the current selection.
 * Lines that have no colour label are skipped silently.
 */
package com.lewisisworking.chevronlists

class RemoveColourLabelAction : ChevronLineTransformAction(
    actionText        = "CL: Remove Colour Label",
    actionDescription = "Strip any {colour} label from this item (preserves all other content)"
) {
    override fun transform(line: String, listPrefix: String): String? =
        computeRemoveColourLabel(line, listPrefix)
}