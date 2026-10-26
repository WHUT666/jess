package com.example.viewmodel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ArenaAccountInfo
import com.example.data.model.AutomationRule
import com.example.data.model.ConversationSessionEntity
import com.example.data.model.ModelIdentityInfo
import com.example.data.model.PromptEntity
import com.example.data.model.SequencingMode
import com.example.data.model.SessionWithTurns
import com.example.data.repository.ArenaRepository
import com.example.engine.ArenaJsBridge
import com.example.engine.BridgeEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AutomationStatus(val label: String) {
    IDLE("待命中"),
    STREAMING("正在生成回复..."),
    COUNTDOWN("检测到停顿，准备引导"),
    SENDING("正在注入预设提示词..."),
    PAUSED("自动引导已暂停"),
    LIMIT_REACHED("已达到设置的最大轮次")
}

data class UiTelemetry(
    val charsDetected: Int = 0,
    val streamingDurationMs: Long = 0,
    val lastModelASnippet: String = "",
    val lastModelBSnippet: String = "",
    val promptsInjectedCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class ArenaPilotViewModel(
    private val repository: ArenaRepository,
    val bridge: ArenaJsBridge,
    private val appContext: Context
) : ViewModel() {

    val allPrompts = repository.allPrompts
    val allLogs = repository.allLogs
    val rules = repository.rulesFlow
    val allConversations = repository.allConversations
    val accountInfo = repository.accountFlow

    private val _status = MutableStateFlow(AutomationStatus.IDLE)
    val status: StateFlow<AutomationStatus> = _status.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _currentTurn = MutableStateFlow(0)
    val currentTurn: StateFlow<Int> = _currentTurn.asStateFlow()

    private val _currentPromptIndex = MutableStateFlow(0)
    val currentPromptIndex: StateFlow<Int> = _currentPromptIndex.asStateFlow()

    private val _telemetry = MutableStateFlow(UiTelemetry())
    val telemetry: StateFlow<UiTelemetry> = _telemetry.asStateFlow()

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    val activeSessionWithTurns: StateFlow<SessionWithTurns?> = _activeSessionId.flatMapLatest { id ->
        if (id != null) {
            repository.getSessionWithTurnsFlow(id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _detectedModelA = MutableStateFlow(ModelIdentityInfo(rawName = "Model A (盲测中)"))
    val detectedModelA: StateFlow<ModelIdentityInfo> = _detectedModelA.asStateFlow()

    private val _detectedModelB = MutableStateFlow(ModelIdentityInfo(rawName = "Model B (盲测中)"))
    val detectedModelB: StateFlow<ModelIdentityInfo> = _detectedModelB.asStateFlow()

    fun getSessionWithTurnsFlow(sessionId: Long) = repository.getSessionWithTurnsFlow(sessionId)

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    var jsEvaluator: ((String) -> Unit)? = null

    private var countdownJob: Job? = null
    private var streamStartTime: Long = 0

    init {
        viewModelScope.launch {
            bridge.events.collectLatest { event ->
                handleBridgeEvent(event)
            }
        }
    }

    private fun handleBridgeEvent(event: BridgeEvent) {
        when (event) {
            is BridgeEvent.StreamProgress -> {
                if (streamStartTime == 0L) {
                    streamStartTime = System.currentTimeMillis()
                }
                val duration = System.currentTimeMillis() - streamStartTime
                _telemetry.value = _telemetry.value.copy(
                    charsDetected = event.totalChars,
                    streamingDurationMs = duration,
                    lastModelASnippet = event.snippetA,
                    lastModelBSnippet = event.snippetB
                )
                if (_status.value != AutomationStatus.STREAMING && _status.value != AutomationStatus.PAUSED) {
                    _status.value = AutomationStatus.STREAMING
                }
                countdownJob?.cancel()
                _countdownSeconds.value = 0
            }

            is BridgeEvent.GenerationStopped -> {
                streamStartTime = 0L
                _telemetry.value = _telemetry.value.copy(
                    charsDetected = event.totalChars,
                    lastModelASnippet = event.snippetA,
                    lastModelBSnippet = event.snippetB
                )
                onGenerationStoppedDetected(event.snippetA, event.snippetB, event.totalChars)
            }

            is BridgeEvent.FullTurnCaptured -> {
                // Save complete turn into conversation management
                viewModelScope.launch {
                    val currentSessId = _activeSessionId.value ?: run {
                        val newId = repository.createNewConversationSession(
                            title = event.userPrompt.take(28),
                            mode = "ARENA_ONLINE",
                            modelA = event.modelAName,
                            modelB = event.modelBName
                        )
                        _activeSessionId.value = newId
                        newId
                    }

                    repository.recordConversationTurn(
                        sessionId = currentSessId,
                        turnIndex = maxOf(1, _currentTurn.value),
                        prompt = event.userPrompt,
                        modelA = event.responseA,
                        modelB = event.responseB,
                        isAutoInjected = _currentTurn.value > 1
                    )
                }
            }

            is BridgeEvent.AccountStateDetected -> {
                val current = accountInfo.value
                repository.updateAccount(
                    current.copy(
                        isLoggedIn = event.isLoggedIn,
                        displayName = if (event.username.isNotBlank()) event.username else current.displayName,
                        email = if (event.email.isNotBlank()) event.email else current.email,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )
            }

            is BridgeEvent.ModelIdentityDetected -> {
                val providerA = ModelIdentityInfo.resolveProvider(event.modelAName)
                val providerB = ModelIdentityInfo.resolveProvider(event.modelBName)
                _detectedModelA.value = ModelIdentityInfo(
                    rawName = event.modelAName,
                    providerFamily = providerA,
                    isRevealed = event.isOfficialReveal,
                    detectionSource = event.detectionSource
                )
                _detectedModelB.value = ModelIdentityInfo(
                    rawName = event.modelBName,
                    providerFamily = providerB,
                    isRevealed = event.isOfficialReveal,
                    detectionSource = event.detectionSource
                )
                _toastMessages.tryEmit("模型识别成功: ${event.modelAName} ⚔️ ${event.modelBName} [${event.detectionSource}]")
            }

            is BridgeEvent.VoteSubmitted -> {
                _toastMessages.tryEmit("已同步提交评测投票: ${event.winner}")
                val curSessId = _activeSessionId.value
                if (curSessId != null) {
                    viewModelScope.launch {
                        repository.updateWinnerVote(curSessId, event.winner.uppercase())
                    }
                }
            }

            is BridgeEvent.PromptInjected -> {
                if (event.success) {
                    _toastMessages.tryEmit("成功自动发送提示词")
                } else {
                    _toastMessages.tryEmit("提示词注入失败: ${event.message}")
                }
                _status.value = AutomationStatus.IDLE
            }

            is BridgeEvent.PageStatus -> {
                _toastMessages.tryEmit("页面就绪: ${event.pageTitle}")
                repository.inspectAndSyncCookies(event.url)
            }

            is BridgeEvent.LogMessage -> {}
        }
    }

    private fun onGenerationStoppedDetected(snippetA: String, snippetB: String, totalChars: Int) {
        val currentRule = rules.value
        if (!currentRule.isAutoSendEnabled) {
            _status.value = AutomationStatus.IDLE
            return
        }

        if (_currentTurn.value >= currentRule.maxRounds) {
            _status.value = AutomationStatus.LIMIT_REACHED
            _toastMessages.tryEmit("已达最大轮次限制 (${currentRule.maxRounds} 轮)，自动引导已安全停止")
            viewModelScope.launch {
                repository.logSession(
                    turnIndex = _currentTurn.value,
                    prompt = "(已达轮次上限)",
                    snippetA = snippetA,
                    snippetB = snippetB,
                    chars = totalChars,
                    status = "LIMIT_REACHED"
                )
            }
            return
        }

        if (currentRule.pauseOnStopKeywords && currentRule.stopKeywords.isNotBlank()) {
            val keywords = currentRule.stopKeywords.split(",", "，")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
            val combined = (snippetA + " " + snippetB).lowercase()
            for (kw in keywords) {
                if (combined.contains(kw)) {
                    _status.value = AutomationStatus.PAUSED
                    _toastMessages.tryEmit("检测到停止关键词: '$kw'，自动引导已暂停")
                    viewModelScope.launch {
                        repository.logSession(
                            turnIndex = _currentTurn.value,
                            prompt = "(触发关键词: $kw)",
                            snippetA = snippetA,
                            snippetB = snippetB,
                            chars = totalChars,
                            status = "STOPPED_BY_KEYWORD"
                        )
                    }
                    return
                }
            }
        }

        startCountdown(currentRule.silenceTimeoutSeconds, snippetA, snippetB, totalChars)
    }

    private fun startCountdown(seconds: Int, snippetA: String, snippetB: String, totalChars: Int) {
        countdownJob?.cancel()
        _status.value = AutomationStatus.COUNTDOWN
        _countdownSeconds.value = seconds

        countdownJob = viewModelScope.launch {
            for (sec in seconds downTo 1) {
                _countdownSeconds.value = sec
                delay(1000)
            }
            _countdownSeconds.value = 0
            executeAutoPrompt(snippetA, snippetB, totalChars)
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _countdownSeconds.value = 0
        _status.value = AutomationStatus.PAUSED
        _toastMessages.tryEmit("已取消本次自动触发")
    }

    fun resumeAutoPilot() {
        _status.value = AutomationStatus.IDLE
        _toastMessages.tryEmit("自动监控引导已恢复")
    }

    fun toggleAutoSend() {
        val currentRule = rules.value
        val updated = currentRule.copy(isAutoSendEnabled = !currentRule.isAutoSendEnabled)
        repository.updateRules(updated)
        if (!updated.isAutoSendEnabled) {
            cancelCountdown()
            _status.value = AutomationStatus.PAUSED
        } else {
            _status.value = AutomationStatus.IDLE
        }
    }

    fun triggerImmediateSend(customPrompt: String? = null) {
        countdownJob?.cancel()
        _countdownSeconds.value = 0
        viewModelScope.launch {
            executeAutoPrompt(
                snippetA = _telemetry.value.lastModelASnippet,
                snippetB = _telemetry.value.lastModelBSnippet,
                totalChars = _telemetry.value.charsDetected,
                overridePrompt = customPrompt
            )
        }
    }

    private suspend fun executeAutoPrompt(
        snippetA: String,
        snippetB: String,
        totalChars: Int,
        overridePrompt: String? = null
    ) {
        val enabledPrompts = repository.getEnabledPrompts()
        if (enabledPrompts.isEmpty() && overridePrompt == null) {
            _toastMessages.tryEmit("提示词库中无启用的提示词，请在词库中启用或添加！")
            _status.value = AutomationStatus.IDLE
            return
        }

        val promptToSend = if (overridePrompt != null) {
            overridePrompt
        } else {
            val mode = rules.value.sequencingMode
            val index = when (mode) {
                SequencingMode.SEQUENTIAL -> {
                    val idx = _currentPromptIndex.value
                    if (idx < enabledPrompts.size) idx else {
                        _status.value = AutomationStatus.LIMIT_REACHED
                        _toastMessages.tryEmit("顺序提示词序列已完成")
                        return
                    }
                }
                SequencingMode.LOOP -> {
                    _currentPromptIndex.value % enabledPrompts.size
                }
                SequencingMode.RANDOM -> {
                    enabledPrompts.indices.random()
                }
                SequencingMode.SMART_DEPTH -> {
                    val smartList = enabledPrompts.filter { it.category.contains("深度") || it.category.contains("辩驳") }
                    val targetPool = smartList.ifEmpty { enabledPrompts }
                    _currentPromptIndex.value % targetPool.size
                }
            }
            val entity = enabledPrompts[index]
            _currentPromptIndex.value = index + 1
            repository.incrementPromptUsage(entity.id)
            entity.content
        }

        _status.value = AutomationStatus.SENDING
        _currentTurn.value += 1

        if (rules.value.soundVibrationEnabled) {
            triggerVibration()
        }

        val jsEscaped = promptToSend
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")

        val script = "if (window.ArenaPilotBridge_injectPrompt) { window.ArenaPilotBridge_injectPrompt('$jsEscaped'); }"
        jsEvaluator?.invoke(script)

        _telemetry.value = _telemetry.value.copy(
            promptsInjectedCount = _telemetry.value.promptsInjectedCount + 1
        )

        repository.logSession(
            turnIndex = _currentTurn.value,
            prompt = promptToSend,
            snippetA = snippetA,
            snippetB = snippetB,
            chars = totalChars,
            status = if (overridePrompt != null) "MANUAL_SENT" else "AUTO_SENT"
        )
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    // === 对话管理与新对战 ===

    fun startNewBattle(customTitle: String = "") {
        countdownJob?.cancel()
        _countdownSeconds.value = 0
        _currentTurn.value = 0
        _currentPromptIndex.value = 0
        _status.value = AutomationStatus.IDLE
        _telemetry.value = UiTelemetry()
        _detectedModelA.value = ModelIdentityInfo(rawName = "Model A (盲测中)")
        _detectedModelB.value = ModelIdentityInfo(rawName = "Model B (盲测中)")

        viewModelScope.launch {
            val title = customTitle.ifBlank { "Arena 对战 ${System.currentTimeMillis() % 10000}" }
            val newSessionId = repository.createNewConversationSession(
                title = title,
                mode = "ARENA_ONLINE"
            )
            _activeSessionId.value = newSessionId

            // Trigger JS to start new battle in Arena web
            val script = "if (window.ArenaPilotBridge_newBattle) { window.ArenaPilotBridge_newBattle(); }"
            jsEvaluator?.invoke(script)

            _toastMessages.tryEmit("已开启新对战会话: $title")
        }
    }

    fun probeModelIdentities() {
        val script = "if (window.ArenaPilotBridge_probeIdentity) { window.ArenaPilotBridge_probeIdentity(); }"
        jsEvaluator?.invoke(script)
        _toastMessages.tryEmit("已注入模型指纹探测引导词，诱导模型自报架构")
    }

    fun selectSession(sessionId: Long) {
        _activeSessionId.value = sessionId
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
            _toastMessages.tryEmit("已删除对战记录")
        }
    }

    fun updateSessionTitle(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
            _toastMessages.tryEmit("会话标题已更新")
        }
    }

    fun voteWinner(sessionId: Long, vote: String) {
        viewModelScope.launch {
            repository.updateWinnerVote(sessionId, vote)
            _toastMessages.tryEmit("已记录投票结果: $vote")
        }
    }

    // === 账户管理 ===

    fun syncCookies(url: String = rules.value.targetUrl) {
        repository.inspectAndSyncCookies(url)
        _toastMessages.tryEmit("已同步当前站点 Cookie 凭据")
    }

    fun updateAccountProfile(displayName: String, token: String) {
        val cur = accountInfo.value
        repository.updateAccount(
            cur.copy(
                displayName = displayName.ifBlank { "Arena 认证用户" },
                isLoggedIn = true,
                customAuthToken = token,
                lastSyncTime = System.currentTimeMillis()
            )
        )
        _toastMessages.tryEmit("账号信息已更新")
    }

    fun logoutAccount() {
        val cur = accountInfo.value
        repository.updateAccount(
            cur.copy(
                isLoggedIn = false,
                displayName = "Arena 访客 (未登录)",
                email = "",
                customAuthToken = "",
                cookieSummary = "已清除凭据"
            )
        )
        _toastMessages.tryEmit("已退出账号")
    }

    fun resetSession() {
        countdownJob?.cancel()
        _countdownSeconds.value = 0
        _currentTurn.value = 0
        _currentPromptIndex.value = 0
        _status.value = AutomationStatus.IDLE
        _telemetry.value = UiTelemetry()
        _toastMessages.tryEmit("对战会话与计数已重置")
    }

    fun updateRules(newRules: AutomationRule) {
        repository.updateRules(newRules)
        _toastMessages.tryEmit("自动化规则设置已保存")
    }

    fun addPrompt(title: String, content: String, category: String) {
        viewModelScope.launch {
            repository.addPrompt(title, content, category)
            _toastMessages.tryEmit("已添加新提示词: $title")
        }
    }

    fun updatePrompt(prompt: PromptEntity) {
        viewModelScope.launch {
            repository.updatePrompt(prompt)
            _toastMessages.tryEmit("提示词已更新")
        }
    }

    fun deletePrompt(prompt: PromptEntity) {
        viewModelScope.launch {
            repository.deletePrompt(prompt)
            _toastMessages.tryEmit("提示词已删除")
        }
    }

    fun resetDefaultPrompts() {
        viewModelScope.launch {
            repository.resetDefaultPrompts()
            _toastMessages.tryEmit("已恢复默认提示词库")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearSessionLogs()
            _toastMessages.tryEmit("历史记录已清空")
        }
    }
}

class ArenaPilotViewModelFactory(
    private val repository: ArenaRepository,
    private val bridge: ArenaJsBridge,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArenaPilotViewModel(repository, bridge, context) as T
    }
}
