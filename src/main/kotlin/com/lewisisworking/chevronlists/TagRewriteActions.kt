/**
 * TagRewriteActions.kt
 * IntelliJ bridge for TagRewrites.kt.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages

private val listPrefix get() = ChevronListsSettings.getInstance().state.listPrefix

private fun validator(error: String, valid: (String) -> Boolean) = object : InputValidatorEx {
    override fun getErrorText(input: String) = if (valid(input.trim())) null else error
    override fun checkInput(input: String) = getErrorText(input) == null
    override fun canClose(input: String) = checkInput(input)
}

private val TAG_NAME = Regex("""#?\w[\w-]*""")

private fun hint(editor: Editor, text: String) = HintManager.getInstance().showInformationHint(editor, text)

private fun caretLine(editor: Editor) = editor.caretModel.logicalPosition.line

private fun apply(e: AnActionEvent, editor: Editor, before: List<String>, after: List<String>) =
    WriteCommandAction.runWriteCommandAction(e.project) { replaceChangedLines(editor.document, before, after) }

private fun plural(count: Int) = if (count == 1) "$count item" else "$count items"

/** Asks which of [tags] to rename and what to call it; null when cancelled */
internal fun askTagRename(e: AnActionEvent, tags: List<String>): Pair<String, String>? {
    val message = "Tag to rename:\n${tags.joinToString(", ") { "#$it" }}"
    val knownTag = validator("No item has that tag") { it.removePrefix("#").lowercase() in tags }
    val old = Messages.showInputDialog(e.project, message, "Rename Tag", null, "", knownTag)?.trim()?.removePrefix("#") ?: return null
    val newName = validator("Use letters, numbers, hyphens and underscores", TAG_NAME::matches)
    val new = Messages.showInputDialog(e.project, "Rename #$old to:", "Rename Tag", null, old, newName)?.trim()?.removePrefix("#") ?: return null
    return if (new == old) null else old to new
}

class RenameTagAction : MarkdownEditorAction(
    "CL: Rename Tag", "Rename a tag on every item in this file"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = editor.document.text.split("\n")
        val tags   = itemTags(before, listPrefix)
        if (tags.isEmpty()) return hint(editor, "CL: No #tags found in this file")
        val (old, new) = askTagRename(e, tags) ?: return
        val edit = renameTagInLines(before, listPrefix, old, new)
        apply(e, editor, before, edit.lines)
        hint(editor, "CL: Renamed #$old to #$new on ${plural(edit.changed)}")
    }
}

class RenameTagSectionAction : MarkdownEditorAction(
    "CL: Rename Tag (Section)", "Rename a tag on the items of this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = editor.document.text.split("\n")
        val tags   = sectionTags(before, caretLine(editor), listPrefix).orEmpty()
        if (tags.isEmpty()) return hint(editor, "CL: No tags found in this section")
        val (old, new) = askTagRename(e, tags) ?: return
        val edit = renameTagInSection(before, caretLine(editor), listPrefix, old, new) ?: return
        apply(e, editor, before, edit.lines)
        hint(editor, "CL: Renamed #$old to #$new on ${plural(edit.changed)}")
    }
}

class ChangeItemPrefixAction : MarkdownEditorAction(
    "CL: Change Item Prefix", "Change the bullet on every item in this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = editor.document.text.split("\n")
        if (headerAtOrAbove(before, caretLine(editor)) == null) return hint(editor, "CL: No section found at caret")
        val choices = ITEM_PREFIXES.joinToString(", ")
        val allowed = validator("Enter one of $choices") { it in ITEM_PREFIXES }
        val prefix  = Messages.showInputDialog(e.project, "New bullet: $choices", "Change Item Prefix", null, "", allowed)?.trim() ?: return
        val edit    = changeSectionPrefix(before, caretLine(editor), listPrefix, prefix) ?: return
        if (edit.changed == 0) return hint(editor, "CL: No bullet items to change in this section")
        apply(e, editor, before, edit.lines)
        hint(editor, "CL: Changed the bullet on ${plural(edit.changed)}")
    }
}

class GroupItemsByTagAction : MarkdownEditorAction(
    "CL: Group Items by Tag", "Cluster this section's items under a heading for each tag"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = editor.document.text.split("\n")
        val after  = groupSectionByTag(before, caretLine(editor), listPrefix)
            ?: return hint(editor, "CL: Not enough distinct tags to group by")
        apply(e, editor, before, after)
    }
}

class SetWordCountGoalAction : MarkdownEditorAction(
    "CL: Set Word Count Goal", "Set or remove the word goal on this section"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val before = editor.document.text.split("\n")
        val header = headerAtOrAbove(before, caretLine(editor)) ?: return hint(editor, "CL: No section found at caret")
        val valid  = validator("Enter a positive whole number, or leave blank to remove the goal") { text ->
            text.isEmpty() || (text.toIntOrNull() ?: 0) > 0
        }
        val current = sectionGoal(before[header])?.toString() ?: ""
        val input   = Messages.showInputDialog(e.project, "Words (blank removes the goal):", "Set Word Count Goal", null, current, valid) ?: return
        val updated = withSectionGoal(before[header], input.trim().toIntOrNull())
        apply(e, editor, before, before.toMutableList().also { it[header] = updated })
    }
}
