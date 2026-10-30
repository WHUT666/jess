package com.example.engine

import com.example.data.model.AccountStatus

/** A result is unknown without an official account-state API or explicit official ban notice. */
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

/** No speculative /api/auth/callback/credentials POST, password use or Cookie copying. */
object ArenaAuthEngine {
    @Suppress("UNUSED_PARAMETER")
    suspend fun probeArenaLoginEndpoints(
        email: String, pass: String, existingCookies: String = ""
    ): ArenaLoginProbeResult = ArenaLoginProbeResult(
        success = false,
        status = AccountStatus.UNCHECKED,
        statusSummary = "未核实；不支持用旧密码推断登录或封禁状态",
        detailLogs = listOf("请在 Arena 官方页面本人完成 Google 或邮箱登录。"),
        recommendedAction = "如官方提示账号被禁用，请向 Arena 客服核实。"
    )

    @Suppress("UNUSED_PARAMETER")
    suspend fun checkAccountBannedStatus(
        email: String, password: String, existingCookies: String = ""
    ): Pair<AccountStatus, String> = AccountStatus.UNCHECKED to "未核实；仅凭 HTTP 状态或 Cookie 不能确认账号封禁"
}
