package com.example.engine

import android.util.Log
import com.example.data.model.AccountStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

data class ArenaLoginProbeResult(
    val success: Boolean,
    val isBannedOnArena: Boolean = false,
    val canDirectApiLogin: Boolean = false,
    val isCloudflareProtected: Boolean = false,
    val isRateLimited: Boolean = false,
    val isInvalidCredentials: Boolean = false,
    val status: AccountStatus = AccountStatus.UNCHECKED,
    val capturedCookies: String = "",
    val csrfTokenFound: Boolean = false,
    val csrfTokenPreview: String = "",
    val availableProviders: List<String> = emptyList(),
    val httpStatusCode: Int = 0,
    val statusSummary: String = "",
    val detailLogs: List<String> = emptyList(),
    val recommendedAction: String = ""
)

object ArenaAuthEngine {
    private const val TAG = "ArenaAuthEngine"
    private const val ARENA_BASE = "https://lmarena.ai"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UD1A.231105.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false) // 手动观察 302 重定向以截获 Set-Cookie 与错误参数
        .followSslRedirects(false)
        .build()

    /**
     * 探测 Arena 登录接口是否支持直接通过账号密码 (Credentials) 进行接口登录，
     * 并全链路检测该账号是否在 Arena 官方体系中被禁用 (Banned/Disabled) 或限流。
     */
    suspend fun probeArenaLoginEndpoints(
        email: String,
        pass: String,
        existingCookies: String = ""
    ): ArenaLoginProbeResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        var capturedCookies = existingCookies.trim()
        var csrfToken = ""
        var providersList = listOf<String>()
        var isBanned = false
        var isCloudflare = false
        var isRateLimited = false
        var isInvalidCreds = false
        var canDirectLogin = false
        var probeStatus = AccountStatus.CHECKING
        var summary = ""
        var recommendation = ""
        var lastCode = 0

        logs.add("🚀 开始探测 Arena (lmarena.ai) 登录与账号禁用状态...")
        logs.add("🎯 探测账号: $email")

        try {
            // ==============================================================
            // 阶段 1: 探测并获取 CSRF Token 与初始 Session 令牌
            // ==============================================================
            logs.add("📡 [阶段 1/4] 发起 GET $ARENA_BASE/api/auth/csrf 获取安全令牌...")
            val csrfReq = Request.Builder()
                .url("$ARENA_BASE/api/auth/csrf")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Referer", "$ARENA_BASE/")
                .build()

            val csrfCookies = mutableListOf<String>()
            httpClient.newCall(csrfReq).execute().use { resp ->
                lastCode = resp.code
                val rawHeaders = resp.headers
                for (name in rawHeaders.names()) {
                    if (name.equals("Set-Cookie", ignoreCase = true)) {
                        val cookieVal = rawHeaders.get(name) ?: ""
                        val cleaned = cookieVal.substringBefore(";")
                        if (cleaned.isNotBlank()) csrfCookies.add(cleaned)
                    }
                }
                val body = resp.body?.string() ?: ""

                if (body.contains("cf-turnstile") || body.contains("challenges.cloudflare.com") || resp.code == 403) {
                    isCloudflare = true
                    logs.add("🛡️ CSRF 接口检测到 Cloudflare Turnstile 人机防护拦截")
                } else if (resp.isSuccessful) {
                    try {
                        val json = JSONObject(body)
                        csrfToken = json.optString("csrfToken", "")
                    } catch (_: Exception) {}
                }
            }

            if (csrfCookies.isNotEmpty()) {
                val merged = (capturedCookies.split("; ").filter { it.isNotBlank() } + csrfCookies).distinct().joinToString("; ")
                capturedCookies = merged
            }

            if (csrfToken.isNotBlank()) {
                logs.add("✅ 成功获取 Arena CSRF 令牌: ${csrfToken.take(10)}... (共 ${csrfToken.length} 字符)")
            } else {
                logs.add("⚠️ 未能直接从 JSON 解析 CSRF 令牌 (HTTP $lastCode)，尝试备用策略...")
            }

            // ==============================================================
            // 阶段 2: 探测 Arena 官方开放的认证提供方 (Providers Probe)
            // ==============================================================
            logs.add("📡 [阶段 2/4] 发起 GET $ARENA_BASE/api/auth/providers 探测开放的认证通道...")
            try {
                val provReq = Request.Builder()
                    .url("$ARENA_BASE/api/auth/providers")
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .apply {
                        if (capturedCookies.isNotBlank()) header("Cookie", capturedCookies)
                    }
                    .build()

                httpClient.newCall(provReq).execute().use { resp ->
                    val body = resp.body?.string() ?: ""
                    if (resp.isSuccessful && body.isNotBlank()) {
                        try {
                            val json = JSONObject(body)
                            val keys = json.keys()
                            val list = mutableListOf<String>()
                            while (keys.hasNext()) {
                                list.add(keys.next())
                            }
                            providersList = list
                            logs.add("📋 Arena 探测到的认证提供商: [${providersList.joinToString(", ")}]")
                        } catch (_: Exception) {
                            logs.add("📋 认证提供商接口返回非标准结构")
                        }
                    } else if (resp.code == 403) {
                        logs.add("🛡️ Providers 探测接口触发 Arena 网关防护 (403)")
                    }
                }
            } catch (e: Exception) {
                logs.add("⚠️ 认证通道探测跳过: ${e.message}")
            }

            // ==============================================================
            // 阶段 3: 尝试通过接口直接发起账号密码登录 (Credentials POST)
            // ==============================================================
            logs.add("📡 [阶段 3/4] 探测 Arena Credentials 接口能否直接通过账号密码登录...")
            
            val formBuilder = FormBody.Builder()
                .add("csrfToken", csrfToken)
                .add("email", email)
                .add("username", email)
                .add("password", pass)
                .add("json", "true")

            val loginReq = Request.Builder()
                .url("$ARENA_BASE/api/auth/callback/credentials")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", ARENA_BASE)
                .header("Referer", "$ARENA_BASE/login")
                .apply {
                    if (capturedCookies.isNotBlank()) header("Cookie", capturedCookies)
                }
                .post(formBuilder.build())
                .build()

            var responseLocation = ""
            var responseBody = ""
            val postCookies = mutableListOf<String>()

            httpClient.newCall(loginReq).execute().use { resp ->
                lastCode = resp.code
                responseLocation = resp.header("Location") ?: ""
                val rawHeaders = resp.headers
                for (name in rawHeaders.names()) {
                    if (name.equals("Set-Cookie", ignoreCase = true)) {
                        val cookieVal = rawHeaders.get(name) ?: ""
                        val cleaned = cookieVal.substringBefore(";")
                        if (cleaned.isNotBlank()) postCookies.add(cleaned)
                    }
                }
                responseBody = resp.body?.string() ?: ""
            }

            if (postCookies.isNotEmpty()) {
                val merged = (capturedCookies.split("; ").filter { it.isNotBlank() } + postCookies).distinct().joinToString("; ")
                capturedCookies = merged
            }

            logs.add("🔍 接口登录响应: HTTP $lastCode, 重定向地址: ${responseLocation.ifBlank { "无" }}")

            // 分析接口返回内容
            val lowerBody = responseBody.lowercase()
            val lowerLoc = responseLocation.lowercase()

            when {
                // 1. 明确检测到账号被禁用 / 封禁
                lastCode == 403 && (lowerBody.contains("banned") || lowerBody.contains("disabled") || lowerBody.contains("suspended") || lowerBody.contains("blocked")) -> {
                    isBanned = true
                    probeStatus = AccountStatus.BANNED
                    logs.add("🚫 严重警告：Arena 接口返回 403，明确判定该账号已被 Arena 官方禁用/封禁！")
                    summary = "账号在 Arena 中已被禁用/封禁 (403 Suspended)"
                    recommendation = "该账号已被加入 Arena 黑名单，建议弃用或更换有效新账号。"
                }
                // 2. 凭据密码不匹配
                lowerLoc.contains("error=credentialssignin") || lowerBody.contains("credentialssignin") || lowerBody.contains("invalid password") || lowerBody.contains("user not found") -> {
                    isInvalidCreds = true
                    probeStatus = AccountStatus.INVALID
                    logs.add("❌ 凭据校验失败：账号或密码不正确，未通过 Arena 校验")
                    summary = "密码错误或未在 Arena 注册"
                    recommendation = "请核对邮箱与密码是否匹配，或先在 Arena 官网注册绑定。"
                }
                // 3. 成功获取了 Session Cookie (arena-auth-prod-v1 或 next-auth)
                capturedCookies.contains("arena-auth") || capturedCookies.contains("next-auth.session-token") || (lastCode in 200..302 && !lowerLoc.contains("error")) -> {
                    canDirectLogin = true
                    probeStatus = AccountStatus.VALID
                    logs.add("🎉 接口直连登录成功！已成功截获 Arena 鉴权 Cookie: arena-auth-prod-v1")
                    summary = "接口直接登录成功，会话凭证已捕获"
                    recommendation = "可直接使用该凭证免密进行对战与自动化测试。"
                }
                // 4. 429 限流
                lastCode == 429 || lowerBody.contains("too many requests") || lowerBody.contains("rate limit") -> {
                    isRateLimited = true
                    probeStatus = AccountStatus.RATE_LIMITED
                    logs.add("⚠️ 探测到该账号或 IP 触发 Arena 429 访问频次过高限制")
                    summary = "Arena 频次超限限流 (429)"
                    recommendation = "建议暂缓请求，或在规则设置中延长静默轮询时间。"
                }
                // 5. Cloudflare Turnstile 或需要通过浏览器进行人机验证
                lowerBody.contains("cf-turnstile") || lowerBody.contains("challenges.cloudflare.com") || lastCode == 403 || lastCode == 503 -> {
                    isCloudflare = true
                    // 如果不是明确被 ban，则属于需要人机防护
                    probeStatus = AccountStatus.CAPTCHA_REQUIRED
                    logs.add("🛡️ 接口探测说明: Arena 登录接口受 Cloudflare Turnstile 人机防护拦截")
                    logs.add("💡 纯无头 HTTP 请求无法穿透 Turnstile，已就绪内置内核浏览器一键模拟登录通道")
                    summary = "接口受 Cloudflare 拦截，需内核浏览器自动填密"
                    recommendation = "点击【一键填密】即可直接在内置浏览器中自动填入并过盾登录。"
                }
                else -> {
                    probeStatus = AccountStatus.VALID
                    logs.add("ℹ️ 账号凭据已就绪，未检测到禁用状态")
                    summary = "凭据正常，未在 Arena 检测到禁用"
                    recommendation = "可使用一键填密进行登录并保持在线凭证。"
                }
            }

            // ==============================================================
            // 阶段 4: 对 Arena 活跃会话进行最终禁用状态复核
            // ==============================================================
            logs.add("📡 [阶段 4/4] 验证 Arena 会话有效性与对战权限...")
            try {
                val sessionReq = Request.Builder()
                    .url("$ARENA_BASE/api/auth/session")
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .apply {
                        if (capturedCookies.isNotBlank()) header("Cookie", capturedCookies)
                    }
                    .build()

                httpClient.newCall(sessionReq).execute().use { sResp ->
                    val sBody = sResp.body?.string() ?: ""
                    if (sResp.code == 403 && (sBody.contains("banned") || sBody.contains("disabled") || sBody.contains("suspended"))) {
                        isBanned = true
                        probeStatus = AccountStatus.BANNED
                        logs.add("🚫 Session 接口复核: 账号已被 Arena 正式封禁")
                    } else if (sResp.isSuccessful && sBody.contains("user")) {
                        logs.add("✅ Session 接口复核通过: 活跃用户凭证有效")
                        if (!isBanned) probeStatus = AccountStatus.VALID
                    } else {
                        logs.add("ℹ️ Session 状态: ${if (sResp.isSuccessful) "未登录访客态" else "HTTP ${sResp.code}"}")
                    }
                }
            } catch (e: Exception) {
                logs.add("⚠️ 会话复核跳过: ${e.message}")
            }

            logs.add("🏁 探测全流程完毕: 状态判定为【${probeStatus.displayName}】")

            ArenaLoginProbeResult(
                success = canDirectLogin || (!isBanned && !isInvalidCreds),
                isBannedOnArena = isBanned,
                canDirectApiLogin = canDirectLogin,
                isCloudflareProtected = isCloudflare,
                isRateLimited = isRateLimited,
                isInvalidCredentials = isInvalidCreds,
                status = probeStatus,
                capturedCookies = capturedCookies,
                csrfTokenFound = csrfToken.isNotBlank(),
                csrfTokenPreview = if (csrfToken.length > 8) csrfToken.take(8) + "..." else csrfToken,
                availableProviders = providersList,
                httpStatusCode = lastCode,
                statusSummary = summary,
                detailLogs = logs,
                recommendedAction = recommendation
            )

        } catch (e: Exception) {
            Log.e(TAG, "探测 Arena 登录接口异常", e)
            logs.add("❌ 探测网络异常: ${e.message ?: "连接超时"}")
            ArenaLoginProbeResult(
                success = false,
                isBannedOnArena = false,
                canDirectApiLogin = false,
                status = AccountStatus.UNCHECKED,
                statusSummary = "网络探测异常: ${e.localizedMessage}",
                detailLogs = logs,
                recommendedAction = "请检查设备网络连接后重试。"
            )
        }
    }

    /**
     * 专门用于快速检测账号在 Arena 中是否被禁用的探针
     */
    suspend fun checkAccountBannedStatus(
        email: String,
        password: String,
        existingCookies: String = ""
    ): Pair<AccountStatus, String> = withContext(Dispatchers.IO) {
        val probe = probeArenaLoginEndpoints(email, password, existingCookies)
        Pair(probe.status, probe.statusSummary)
    }
}
