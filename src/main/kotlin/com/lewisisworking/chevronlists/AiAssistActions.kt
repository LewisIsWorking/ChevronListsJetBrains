/**
 * AiAssistActions.kt
 * IntelliJ bridge for AiAssist.kt: asks Claude, in a background task, to
 * suggest items for a section, summarise it, or expand an item into sub-items.
 * The API key lives in the IDE's password safe (Settings, Tools, Chevron Lists),
 * with the ANTHROPIC_API_KEY environment variable as a fallback.
 */
package com.lewisisworking.chevronlists

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.messages.BetaFallbacksParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.intellij.codeInsight.hint.HintManager
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.time.Duration
import javax.swing.JComponent

/** The Anthropic API key, kept in the IDE's password safe rather than in settings XML */
object AnthropicApiKey {
    private val attributes get() = CredentialAttributes(generateServiceName("Chevron Lists", "Anthropic API key"))

    /** The stored key, or blank. May be slow (OS keychain): keep it off the EDT where possible */
    var stored: String
        get() = PasswordSafe.instance.getPassword(attributes) ?: ""
        set(value) = PasswordSafe.instance.setPassword(attributes, value.trim().ifEmpty { null })

    /** The key to use: the stored one, else ANTHROPIC_API_KEY, else null */
    fun resolve(): String? = stored.ifBlank { System.getenv("ANTHROPIC_API_KEY") ?: "" }.ifBlank { null }
}

/** Thrown with a message fit to show the user */
class AiAssistException(message: String) : Exception(message)

/** Sends one system prompt and user message to Claude and returns the text of the reply */
private fun askClaude(system: String, user: String): String {
    val key = AnthropicApiKey.resolve()
        ?: throw AiAssistException("No Anthropic API key. Add one in Settings, Tools, Chevron Lists.")
    val client = AnthropicOkHttpClient.builder().apiKey(key).timeout(Duration.ofSeconds(120)).build()
    try {
        val params = MessageCreateParams.builder()
            .model(Model.CLAUDE_OPUS_5_5)
            .maxTokens(16000L)
            .system(system)
            .addUserMessage(user)
            // Short list edits need little thinking: low effort keeps them quick and cheap
            .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.LOW).build())
            // If the model declines, let the server retry on its default fallback model
            .addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
            .fallbacks(BetaFallbacksParam.ofDefault())
            .build()
        val message = try {
            client.beta().messages().create(params)
        } catch (e: AnthropicServiceException) {
            throw AiAssistException(aiErrorMessage(e.statusCode(), e.message ?: ""))
        } catch (e: Exception) {
            throw AiAssistException(aiErrorMessage(null, e.message ?: e.javaClass.simpleName))
        }
        if (message.stopReason().orElse(null) == BetaStopReason.REFUSAL)
            throw AiAssistException("Claude declined this request.")
        return message.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
    } finally {
        client.close()
    }
}

/** Asks Claude in a cancellable background task, then hands the reply to [onReply] on the EDT */
private fun askInBackground(editor: Editor, title: String, system: String, user: String, onReply: (String) -> Unit) {
    object : Task.Backgroundable(editor.project, title, true) {
        private var reply = ""
        override fun run(indicator: ProgressIndicator) { reply = askClaude(system, user) }
        override fun onSuccess() { if (!editor.isDisposed) onReply(reply) }
        override fun onThrowable(error: Throwable) {
            val text = if (error is AiAssistException) error.message else "Claude request failed: ${error.message}"
            Messages.showErrorDialog(editor.project, text, "Chevron Lists")
        }
    }.queue()
}

/**
 * Inserts [lines] after the line the [anchor] marker sits at the end of. The
 * marker tracks edits made while Claude was thinking, so the lines still land
 * in the right place.
 */
private fun insertAfter(editor: Editor, anchor: RangeMarker, lines: List<String>) {
    if (!anchor.isValid || lines.isEmpty()) return
    WriteCommandAction.runWriteCommandAction(editor.project, "CL: Insert Claude's Items", null, {
        val doc = editor.document
        doc.insertString(doc.getLineEndOffset(doc.getLineNumber(anchor.endOffset)), "\n" + lines.joinToString("\n"))
    })
    anchor.dispose()
}

private fun lineEndMarker(editor: Editor, line: Int): RangeMarker {
    val offset = editor.document.getLineEndOffset(line)
    return editor.document.createRangeMarker(offset, offset)
}

private fun listPrefix() = ChevronListsSettings.getInstance().state.listPrefix

private fun caretLines(editor: Editor) =
    editor.document.text.split("\n") to editor.caretModel.logicalPosition.line

class SuggestItemsAction : MarkdownEditorAction(
    "CL: Suggest Items (AI)",
    "Ask Claude for more items for this section, then pick which to add"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = listPrefix()
        val (lines, caret) = caretLines(editor)
        val section = aiSection(lines, caret, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "No section header above the caret")
        val anchor = lineEndMarker(editor, section.contentEnd)
        askInBackground(editor, "CL: Asking Claude for suggestions", AI_SYSTEM_SUGGEST, suggestMessage(section)) { reply ->
            val suggestions = replyItems(reply)
            if (suggestions.isEmpty()) return@askInBackground HintManager.getInstance().showInformationHint(editor, "Claude suggested nothing")
            val picks = SuggestionsDialog(editor, suggestions).pick()
            insertAfter(editor, anchor, itemLines(picks, ">>", prefix))
        }
    }
}

class SummariseSectionAction : MarkdownEditorAction(
    "CL: Summarise Section (AI)",
    "Ask Claude for a one-line summary of this section and add it under the header"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = listPrefix()
        val (lines, caret) = caretLines(editor)
        val section = aiSection(lines, caret, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "No section header above the caret")
        val anchor = lineEndMarker(editor, section.headerLine)
        askInBackground(editor, "CL: Summarising section", AI_SYSTEM_SUMMARISE, summariseMessage(section)) { reply ->
            insertAfter(editor, anchor, listOfNotNull(summaryLine(reply, prefix)))
        }
    }
}

class ExpandItemAction : MarkdownEditorAction(
    "CL: Expand Item (AI)",
    "Ask Claude to break the item at the caret into sub-items"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val prefix = listPrefix()
        val (lines, caret) = caretLines(editor)
        val item = aiItem(lines, caret, prefix)
            ?: return HintManager.getInstance().showInformationHint(editor, "Put the caret on a chevron item to expand it")
        val anchor = lineEndMarker(editor, caret)
        askInBackground(editor, "CL: Expanding item", AI_SYSTEM_EXPAND, expandMessage(item)) { reply ->
            val subItems = replyItems(reply)
            if (subItems.isEmpty()) return@askInBackground HintManager.getInstance().showInformationHint(editor, "Claude suggested no sub-items")
            insertAfter(editor, anchor, itemLines(subItems, item.chevrons + ">", prefix))
        }
    }
}

/** Every suggestion, ticked; the user unticks the ones not wanted */
private class SuggestionsDialog(editor: Editor, private val suggestions: List<String>) : DialogWrapper(editor.project) {
    private val list = CheckBoxList<String>().apply { suggestions.forEach { addItem(it, it, true) } }

    init {
        title = "Claude's Suggestions"
        setOKButtonText("Add")
        init()
    }

    override fun createCenterPanel(): JComponent = JBScrollPane(list).apply { preferredSize = JBUI.size(420, 200) }

    /** Shows the dialog; the ticked suggestions, or none when cancelled */
    fun pick(): List<String> =
        if (showAndGet()) suggestions.filterIndexed { i, _ -> list.isItemSelected(i) } else emptyList()
}
