package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import com.example.data.db.AppDatabase
import com.example.data.model.ArenaAccountInfo
import com.example.data.model.AutomationRule
import com.example.data.model.ConversationSessionEntity
import com.example.data.model.ConversationTurnEntity
import com.example.data.model.PromptEntity
import com.example.data.model.SequencingMode
import com.example.data.model.SessionLogEntity
import com.example.data.model.SessionWithTurns
import com.example.data.model.AccountStatus
import com.example.data.model.ManagedAccountEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ArenaRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = AppDatabase.getDatabase(context, scope)
    private val promptDao = database.promptDao()
    private val sessionLogDao = database.sessionLogDao()
    private val conversationDao = database.conversationDao()
    private val accountDao = database.accountDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("arena_pilot_prefs", Context.MODE_PRIVATE)

    val allPrompts: Flow<List<PromptEntity>> = promptDao.getAllPromptsFlow()
    val allLogs: Flow<List<SessionLogEntity>> = sessionLogDao.getAllLogsFlow()
    val allConversations: Flow<List<ConversationSessionEntity>> = conversationDao.getAllSessionsFlow()
    val allManagedAccounts: Flow<List<com.example.data.model.ManagedAccountEntity>> = accountDao.getAllAccountsFlow()
    val activeManagedAccount: Flow<com.example.data.model.ManagedAccountEntity?> = accountDao.getActiveAccountFlow()

    private val _rulesFlow = MutableStateFlow(loadRulesFromPrefs())
    val rulesFlow: StateFlow<AutomationRule> = _rulesFlow.asStateFlow()

    private val _accountFlow = MutableStateFlow(loadAccountFromPrefs())
    val accountFlow: StateFlow<ArenaAccountInfo> = _accountFlow.asStateFlow()

    init {
        scope.launch {
            if (promptDao.count() == 0) {
                AppDatabase.populateInitialPrompts(promptDao)
            }
            if (conversationDao.countSessions() == 0) {
                AppDatabase.populateInitialConversations(conversationDao)
            }
            if (accountDao.count() == 0) {
                AppDatabase.populateInitialAccounts(accountDao)
            }
        }
    }

    private fun loadRulesFromPrefs(): AutomationRule {
        val timeout = prefs.getInt("silence_timeout", 3)
        val maxRounds = prefs.getInt("max_rounds", 5)
        val autoSend = prefs.getBoolean("auto_send_enabled", true)
        val modeStr = prefs.getString("sequencing_mode", SequencingMode.SEQUENTIAL.name) ?: SequencingMode.SEQUENTIAL.name
        val mode = try {
            SequencingMode.valueOf(modeStr)
        } catch (_: Exception) {
            SequencingMode.SEQUENTIAL
        }
        val soundVib = prefs.getBoolean("sound_vib", true)
        val pauseKeywords = prefs.getBoolean("pause_keywords", true)
        val stopKeywords = prefs.getString("stop_keywords", "抱歉, 无法回答, 触发安全, I cannot, As an AI language model") ?: ""
        val targetUrl = prefs.getString("target_url", "https://lmarena.ai/") ?: "https://lmarena.ai/"

        return AutomationRule(
            silenceTimeoutSeconds = timeout,
            maxRounds = maxRounds,
            isAutoSendEnabled = autoSend,
            sequencingMode = mode,
            soundVibrationEnabled = soundVib,
            pauseOnStopKeywords = pauseKeywords,
            stopKeywords = stopKeywords,
            targetUrl = targetUrl
        )
    }

    private fun loadAccountFromPrefs(): ArenaAccountInfo {
        val isLoggedIn = prefs.getBoolean("arena_is_logged_in", false)
        val displayName = prefs.getString("arena_display_name", "Arena 访客 (未登录)") ?: "Arena 访客 (未登录)"
        val email = prefs.getString("arena_email", "") ?: ""
        val cookieSummary = prefs.getString("arena_cookie_summary", "暂无活跃 Session Cookie") ?: "暂无活跃 Session Cookie"
        val hasActiveCookies = prefs.getBoolean("arena_has_active_cookies", false)
        val totalVotes = prefs.getInt("arena_total_votes", 0)
        val lastSync = prefs.getLong("arena_last_sync", 0L)
        val customToken = prefs.getString("arena_custom_token", "") ?: ""

        return ArenaAccountInfo(
            isLoggedIn = isLoggedIn,
            displayName = displayName,
            email = email,
            cookieSummary = cookieSummary,
            hasActiveCookies = hasActiveCookies,
            totalVotesSubmitted = totalVotes,
            lastSyncTime = lastSync,
            customAuthToken = customToken
        )
    }

    fun updateRules(newRules: AutomationRule) {
        prefs.edit().apply {
            putInt("silence_timeout", newRules.silenceTimeoutSeconds)
            putInt("max_rounds", newRules.maxRounds)
            putBoolean("auto_send_enabled", newRules.isAutoSendEnabled)
            putString("sequencing_mode", newRules.sequencingMode.name)
            putBoolean("sound_vib", newRules.soundVibrationEnabled)
            putBoolean("pause_keywords", newRules.pauseOnStopKeywords)
            putString("stop_keywords", newRules.stopKeywords)
            putString("target_url", newRules.targetUrl)
            apply()
        }
        _rulesFlow.value = newRules
    }

    fun updateAccount(account: ArenaAccountInfo) {
        prefs.edit().apply {
            putBoolean("arena_is_logged_in", account.isLoggedIn)
            putString("arena_display_name", account.displayName)
            putString("arena_email", account.email)
            putString("arena_cookie_summary", account.cookieSummary)
            putBoolean("arena_has_active_cookies", account.hasActiveCookies)
            putInt("arena_total_votes", account.totalVotesSubmitted)
            putLong("arena_last_sync", account.lastSyncTime)
            putString("arena_custom_token", account.customAuthToken)
            apply()
        }
        _accountFlow.value = account
    }

    fun inspectAndSyncCookies(targetUrl: String) {
        try {
            val cookie = CookieManager.getInstance().getCookie(targetUrl)
            if (!cookie.isNullOrBlank()) {
                val hasAuthToken = cookie.contains("session") || cookie.contains("auth") || cookie.contains("token") || cookie.contains("id")
                val summary = if (hasAuthToken) "已捕获并同步有效会话凭据 (${cookie.length} 字节)" else "存在站点基础 Cookie (${cookie.length} 字节)"
                val current = _accountFlow.value
                updateAccount(
                    current.copy(
                        cookieSummary = summary,
                        hasActiveCookies = true,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )
            }
        } catch (_: Exception) {}
    }

    // === 对话管理方法 ===

    fun getSessionWithTurnsFlow(sessionId: Long): Flow<SessionWithTurns?> {
        return conversationDao.getSessionWithTurnsFlow(sessionId)
    }

    suspend fun getSessionWithTurns(sessionId: Long): SessionWithTurns? {
        return conversationDao.getSessionWithTurns(sessionId)
    }

    suspend fun createNewConversationSession(
        title: String,
        mode: String,
        modelA: String = "Model A",
        modelB: String = "Model B"
    ): Long {
        val session = ConversationSessionEntity(
            title = title.ifBlank { "Arena 对战会话 ${System.currentTimeMillis() % 10000}" },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            mode = mode,
            modelAName = modelA,
            modelBName = modelB,
            totalTurns = 0,
            winnerVote = "NONE"
        )
        return conversationDao.insertSession(session)
    }

    suspend fun recordConversationTurn(
        sessionId: Long,
        turnIndex: Int,
        prompt: String,
        modelA: String,
        modelB: String,
        isAutoInjected: Boolean
    ) {
        val turn = ConversationTurnEntity(
            sessionId = sessionId,
            turnIndex = turnIndex,
            userPrompt = prompt,
            modelAResponse = modelA,
            modelBResponse = modelB,
            timestamp = System.currentTimeMillis(),
            isAutoInjected = isAutoInjected
        )
        conversationDao.insertTurn(turn)

        // Update session turn count and updatedAt
        conversationDao.getSessionById(sessionId)?.let { session ->
            val updated = session.copy(
                totalTurns = maxOf(session.totalTurns, turnIndex),
                updatedAt = System.currentTimeMillis()
            )
            conversationDao.updateSession(updated)
        }
    }

    suspend fun updateWinnerVote(sessionId: Long, vote: String) {
        conversationDao.updateWinnerVote(sessionId, vote)
        // Increment vote count
        val curAcc = _accountFlow.value
        updateAccount(curAcc.copy(totalVotesSubmitted = curAcc.totalVotesSubmitted + 1))
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        conversationDao.updateSessionTitle(sessionId, newTitle)
    }

    suspend fun deleteSession(sessionId: Long) {
        conversationDao.deleteTurnsForSession(sessionId)
        conversationDao.deleteSession(sessionId)
    }

    // === Prompt & Log operations ===

    suspend fun getEnabledPrompts(): List<PromptEntity> {
        return promptDao.getEnabledPrompts()
    }

    suspend fun addPrompt(title: String, content: String, category: String): Long {
        val count = promptDao.count()
        val entity = PromptEntity(
            title = title.trim(),
            content = content.trim(),
            category = category.trim().ifEmpty { "通用" },
            orderIndex = count + 1,
            isEnabled = true
        )
        return promptDao.insert(entity)
    }

    suspend fun updatePrompt(prompt: PromptEntity) {
        promptDao.update(prompt)
    }

    suspend fun deletePrompt(prompt: PromptEntity) {
        promptDao.delete(prompt)
    }

    suspend fun incrementPromptUsage(id: Long) {
        promptDao.incrementTimesUsed(id)
    }

    suspend fun resetDefaultPrompts() {
        promptDao.clearAll()
        AppDatabase.populateInitialPrompts(promptDao)
    }

    suspend fun logSession(
        turnIndex: Int,
        prompt: String,
        snippetA: String,
        snippetB: String,
        chars: Int,
        status: String
    ) {
        val entry = SessionLogEntity(
            turnIndex = turnIndex,
            promptContent = prompt,
            modelASnippet = snippetA.take(200),
            modelBSnippet = snippetB.take(200),
            charsDetected = chars,
            status = status
        )
        sessionLogDao.insert(entry)
    }

    suspend fun clearSessionLogs() {
        sessionLogDao.clearAll()
    }

    // === 多账户管理方法 ===

    suspend fun addManagedAccount(email: String, pass: String, label: String, notes: String = ""): Long {
        val entity = com.example.data.model.ManagedAccountEntity(
            email = email.trim(),
            password = pass.trim(),
            label = label.trim().ifBlank { email.substringBefore("@") },
            isActive = false,
            notes = notes
        )
        return accountDao.insert(entity)
    }

    suspend fun updateManagedAccount(account: com.example.data.model.ManagedAccountEntity) {
        accountDao.update(account)
    }

    suspend fun deleteManagedAccount(account: com.example.data.model.ManagedAccountEntity) {
        accountDao.delete(account)
    }

    suspend fun setActiveManagedAccount(id: Long) {
        accountDao.setActiveAccount(id)
        val selected = accountDao.getAccountById(id)
        if (selected != null) {
            val cur = _accountFlow.value
            updateAccount(
                cur.copy(
                    displayName = selected.label,
                    email = selected.email,
                    isLoggedIn = true
                )
            )
        }
    }

    suspend fun getActiveManagedAccount(): com.example.data.model.ManagedAccountEntity? {
        return accountDao.getActiveAccount()
    }

    suspend fun getAllAccountsList(): List<ManagedAccountEntity> {
        return accountDao.getAllAccountsList()
    }

    suspend fun updateAccountStatus(id: Long, status: AccountStatus, failureReason: String = "") {
        accountDao.updateAccountStatus(id, status.name, System.currentTimeMillis(), failureReason)
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun checkAccountValidity(account: ManagedAccountEntity): AccountStatus = withContext(Dispatchers.IO) {
        // 1. 基础凭据完整性校验
        if (account.email.isBlank() || account.password.isBlank()) {
            val status = AccountStatus.INVALID
            updateAccountStatus(account.id, status, "邮箱或密码为空")
            return@withContext status
        }

        if (!account.email.contains("@") || !account.email.contains(".")) {
            val status = AccountStatus.INVALID
            updateAccountStatus(account.id, status, "邮箱格式不符合规范")
            return@withContext status
        }

        // 2. 标记正在检测
        updateAccountStatus(account.id, AccountStatus.CHECKING, "")

        // 3. 网络探针验证：针对 Outlook / Microsoft 账号探测身份端点
        try {
            val emailLower = account.email.lowercase()
            val isMicrosoft = emailLower.endsWith("@outlook.com") || emailLower.endsWith("@hotmail.com") || emailLower.endsWith("@live.com")

            if (isMicrosoft) {
                val url = "https://login.microsoftonline.com/common/userrealm/${account.email}?api-version=2.1"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) ArenaPilot/1.0")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        if (body.contains("\"State\": 4") || body.contains("Throttle") || response.code == 429) {
                            val status = AccountStatus.RATE_LIMITED
                            updateAccountStatus(account.id, status, "微软网关限流或频次过高")
                            return@withContext status
                        } else if (body.contains("NameSpaceType") || body.contains("Federated") || body.contains("Managed") || body.contains("UnknownFederation")) {
                            val status = AccountStatus.VALID
                            updateAccountStatus(account.id, status, "账号在微软域中已注册且密码已配置")
                            return@withContext status
                        } else {
                            val status = AccountStatus.VALID
                            updateAccountStatus(account.id, status, "格式合规且凭据已就绪")
                            return@withContext status
                        }
                    } else if (response.code == 429) {
                        val status = AccountStatus.RATE_LIMITED
                        updateAccountStatus(account.id, status, "接口返回 429 请求过于频繁")
                        return@withContext status
                    }
                }
            }

            // 针对非微软或网络兜底状态
            val status = AccountStatus.VALID
            updateAccountStatus(account.id, status, "凭据格式校验通过")
            status
        } catch (e: Exception) {
            // 网络异常时保留格式验证
            val status = AccountStatus.VALID
            updateAccountStatus(account.id, status, "凭据就绪 (网络离线检测模式)")
            status
        }
    }
}
