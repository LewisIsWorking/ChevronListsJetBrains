/**
 * ChevronEnterHandler.kt
 * Bridges the pure `computeEnterAction` logic to the IntelliJ EnterHandler
 * extension point. The handler reads the current line, decides what to do
 * via the pure function, and applies the edit through a WriteCommandAction.
 */
package com.lewisisworking.chevronlists

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class ChevronEnterHandler : EnterHandlerDelegate {

    override fun preprocessEnter(
        file:            PsiFile,
        editor:          Editor,
        caretOffset:     Ref<Int>,
        caretAdvance:    Ref<Int>,
        dataContext:     DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result {
        if (!file.name.endsWith(".md")) return EnterHandlerDelegate.Result.Continue

        val document   = editor.document
        val offset     = caretOffset.get()
        val lineNumber = document.getLineNumber(offset)
        val lineStart  = document.getLineStartOffset(lineNumber)
        val lineEnd    = document.getLineEndOffset(lineNumber)
        val lineText   = document.getText(TextRange(lineStart, lineEnd))

        return when (val action = computeEnterAction(lineText, settingsPrefix(), settingsListType())) {
            is EnterAction.Default  -> EnterHandlerDelegate.Result.Continue
            is EnterAction.EndList  -> handleEndList(file, editor, lineStart, offset)
            is EnterAction.Continue -> handleContinue(file, editor, offset, caretOffset, action.insert)
        }
    }

    /**
     * Required by the extension point, and intentionally a no-op -- all of the
     * chevron logic runs in [preprocessEnter].
     *
     * This must be implemented explicitly. On IntelliJ Platform 2025.2+ the
     * interface supplies a default body, but on 2024.3 (243) and 2025.1 (251)
     * `postProcessEnter` is still abstract. Compiling against 2025.2 therefore
     * produced a plugin that passed the build but threw AbstractMethodError the
     * first time a user pressed Enter in a markdown file on those older IDEs --
     * which the plugin claims to support via since-build = 243. Returning
     * `Continue` matches the newer platforms' default behaviour, so this is
     * correct on every supported version.
     */
    override fun postProcessEnter(
        file:        PsiFile,
        editor:      Editor,
        dataContext: DataContext
    ): EnterHandlerDelegate.Result = EnterHandlerDelegate.Result.Continue

    /** Reads the bullet prefix from persistent settings (default "-") */
    private fun settingsPrefix(): String = ChevronListsSettings.getInstance().state.listPrefix

    /** Reads the default new list type from persistent settings (default "unordered") */
    private fun settingsListType(): String = ChevronListsSettings.getInstance().state.defaultNewListType

    /** Clear the empty list-item line, then let the default Enter handler add a fresh newline */
    private fun handleEndList(file: PsiFile, editor: Editor, lineStart: Int, caretOffset: Int): EnterHandlerDelegate.Result {
        WriteCommandAction.runWriteCommandAction(file.project) {
            editor.document.replaceString(lineStart, caretOffset, "")
        }
        return EnterHandlerDelegate.Result.Continue
    }

    /** Insert `\n` + continuation text at the caret and stop further processing */
    private fun handleContinue(
        file: PsiFile, editor: Editor, offset: Int,
        caretOffsetRef: Ref<Int>, insertText: String
    ): EnterHandlerDelegate.Result {
        WriteCommandAction.runWriteCommandAction(file.project) {
            editor.document.insertString(offset, "\n$insertText")
        }
        val newOffset = offset + 1 + insertText.length
        editor.caretModel.moveToOffset(newOffset)
        caretOffsetRef.set(newOffset)
        return EnterHandlerDelegate.Result.Stop
    }
}