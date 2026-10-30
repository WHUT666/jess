package com.example.engine

import java.net.URI
import java.util.Locale

/** Only the official, user-facing Agent route is resumable. Never persist auth query strings. */
object AgentPagePolicy {
    const val AGENT_URL = "https://arena.ai/agent"

    fun isOfficialHost(host: String?): Boolean {
        val name = host?.lowercase(Locale.ROOT) ?: return false
        return name == "arena.ai" || name.endsWith(".arena.ai") ||
            name == "lmarena.ai" || name.endsWith(".lmarena.ai")
    }

    fun resumableUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = URI(url)
            if (uri.scheme?.lowercase(Locale.ROOT) != "https" ||
                uri.host?.lowercase(Locale.ROOT) != "arena.ai" ||
                uri.userInfo != null || (uri.port != -1 && uri.port != 443)
            ) return null
            val path = uri.rawPath.orEmpty()
            if (path != "/agent" && !path.startsWith("/agent/")) return null
            // No fragments or queries: callback URLs can contain sensitive one-time values.
            "https://arena.ai$path"
        } catch (_: Exception) {
            null
        }
    }

    /** A gateway code is never proof of an account ban. */
    fun accessMessage(httpStatus: Int): String = when (httpStatus) {
        401, 403 -> "Arena 拒绝了本次页面访问（HTTP $httpStatus）；可能是登录或网络限制，不能据此判定封禁。请在官网核实。"
        429 -> "Arena 返回访问频率限制（HTTP 429）；请等待官方限制解除，不要切换账号绕过。"
        else -> "Arena 页面请求失败（HTTP $httpStatus）；账号状态未核实。"
    }

    const val AGENT_MODEL_STATUS = "Agent 模型未公开（无官方可验证身份）"
}
