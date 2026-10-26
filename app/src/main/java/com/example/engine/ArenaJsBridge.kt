package com.example.engine

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface BridgeEvent {
    data class StreamProgress(
        val totalChars: Int,
        val isStreaming: Boolean,
        val snippetA: String,
        val snippetB: String
    ) : BridgeEvent

    data class GenerationStopped(
        val totalChars: Int,
        val snippetA: String,
        val snippetB: String,
        val reason: String
    ) : BridgeEvent

    data class FullTurnCaptured(
        val userPrompt: String,
        val responseA: String,
        val responseB: String,
        val modelAName: String,
        val modelBName: String
    ) : BridgeEvent

    data class AccountStateDetected(
        val isLoggedIn: Boolean,
        val username: String,
        val email: String
    ) : BridgeEvent

    data class PromptInjected(
        val prompt: String,
        val success: Boolean,
        val message: String
    ) : BridgeEvent

    data class PageStatus(
        val isReady: Boolean,
        val pageTitle: String,
        val url: String
    ) : BridgeEvent

    data class ModelIdentityDetected(
        val modelAName: String,
        val modelBName: String,
        val isOfficialReveal: Boolean,
        val detectionSource: String
    ) : BridgeEvent

    data class VoteSubmitted(
        val winner: String // "model_a", "model_b", "tie", "both_bad"
    ) : BridgeEvent

    data class LogMessage(
        val level: String,
        val message: String
    ) : BridgeEvent
}

class ArenaJsBridge {
    private val _events = MutableSharedFlow<BridgeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BridgeEvent> = _events.asSharedFlow()

    @JavascriptInterface
    fun onModelIdentityDetected(modelAName: String, modelBName: String, isOfficialReveal: Boolean, detectionSource: String) {
        _events.tryEmit(
            BridgeEvent.ModelIdentityDetected(
                modelAName = modelAName,
                modelBName = modelBName,
                isOfficialReveal = isOfficialReveal,
                detectionSource = detectionSource
            )
        )
    }

    @JavascriptInterface
    fun onStreamProgress(totalChars: Int, isStreaming: Boolean, snippetA: String, snippetB: String) {
        _events.tryEmit(
            BridgeEvent.StreamProgress(
                totalChars = totalChars,
                isStreaming = isStreaming,
                snippetA = snippetA,
                snippetB = snippetB
            )
        )
    }

    @JavascriptInterface
    fun onGenerationStopped(totalChars: Int, snippetA: String, snippetB: String, reason: String) {
        _events.tryEmit(
            BridgeEvent.GenerationStopped(
                totalChars = totalChars,
                snippetA = snippetA,
                snippetB = snippetB,
                reason = reason
            )
        )
    }

    @JavascriptInterface
    fun onFullTurnCaptured(userPrompt: String, responseA: String, responseB: String, modelAName: String, modelBName: String) {
        _events.tryEmit(
            BridgeEvent.FullTurnCaptured(
                userPrompt = userPrompt,
                responseA = responseA,
                responseB = responseB,
                modelAName = modelAName,
                modelBName = modelBName
            )
        )
    }

    @JavascriptInterface
    fun onAccountDetected(isLoggedIn: Boolean, username: String, email: String) {
        _events.tryEmit(
            BridgeEvent.AccountStateDetected(
                isLoggedIn = isLoggedIn,
                username = username,
                email = email
            )
        )
    }

    @JavascriptInterface
    fun onVoteSubmitted(winner: String) {
        _events.tryEmit(
            BridgeEvent.VoteSubmitted(winner = winner)
        )
    }

    @JavascriptInterface
    fun onPromptInjected(prompt: String, success: Boolean, message: String) {
        _events.tryEmit(
            BridgeEvent.PromptInjected(
                prompt = prompt,
                success = success,
                message = message
            )
        )
    }

    @JavascriptInterface
    fun onPageReady(pageTitle: String, url: String) {
        _events.tryEmit(
            BridgeEvent.PageStatus(
                isReady = true,
                pageTitle = pageTitle,
                url = url
            )
        )
    }

    @JavascriptInterface
    fun onLog(level: String, message: String) {
        _events.tryEmit(
            BridgeEvent.LogMessage(
                level = level,
                message = message
            )
        )
    }
}
