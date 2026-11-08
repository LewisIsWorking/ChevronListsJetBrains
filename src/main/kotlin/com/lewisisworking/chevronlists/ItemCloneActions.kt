/**
 * ItemCloneActions.kt
 * IntelliJ bridge for ItemClones.kt.
 */
package com.lewisisworking.chevronlists

class CloneItemAction : ChevronItemCopyAction(
    "CL: Clone Item", "Copy this item to the end of its section; a numbered copy continues the list"
) {
    override fun compute(lines: List<String>, index: Int, listPrefix: String) =
        cloneItemToSectionEnd(lines, index, listPrefix)
}

class CloneItemAsDoneAction : ChevronItemCopyAction(
    "CL: Clone Item as Done", "Copy this item below itself with its checkbox ticked"
) {
    override fun compute(lines: List<String>, index: Int, listPrefix: String) =
        itemCopyBelow(lines, index, listPrefix, ::markCloneDone)
}

class CloneItemStrippedAction : ChevronItemCopyAction(
    "CL: Clone Item Stripped", "Copy this item below itself with every marker removed"
) {
    override fun compute(lines: List<String>, index: Int, listPrefix: String) =
        itemCopyBelow(lines, index, listPrefix, ::stripItemMetadata)
}

class DuplicateItemAndIncrementAction : ChevronItemCopyAction(
    "CL: Duplicate Item and Increment", "Copy this item below itself with the first number in its text one higher"
) {
    override fun compute(lines: List<String>, index: Int, listPrefix: String) =
        itemCopyBelow(lines, index, listPrefix) { incrementFirstNumber(it) ?: it }
}
