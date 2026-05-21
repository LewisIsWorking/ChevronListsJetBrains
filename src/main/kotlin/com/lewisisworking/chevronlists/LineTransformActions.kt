/**
 * LineTransformActions.kt
 * Concrete line-transform actions. Each is a one-line subclass of
 * ChevronLineTransformAction that delegates to a pure compute* function.
 */
package com.lewisisworking.chevronlists

class PromoteItemAction : ChevronLineTransformAction(
    actionText        = "CL: Promote Item",
    actionDescription = "Decrease this item's chevron depth (move it toward the top of the hierarchy)"
) {
    override fun transform(line: String, listPrefix: String): String? =
        computePromote(line, listPrefix)
}

class DemoteItemAction : ChevronLineTransformAction(
    actionText        = "CL: Demote Item",
    actionDescription = "Increase this item's chevron depth (nest it one level deeper)"
) {
    override fun transform(line: String, listPrefix: String): String? =
        computeDemote(line, listPrefix)
}

class CycleListTypeAction : ChevronLineTransformAction(
    actionText        = "CL: Cycle List Type",
    actionDescription = "Toggle this item between bullet (>> -) and numbered (>> 1.) form"
) {
    override fun transform(line: String, listPrefix: String): String? =
        computeCycleListType(line, listPrefix)
}