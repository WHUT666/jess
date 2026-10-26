package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String, // e.g. "交叉辩驳", "深度推演", "边界压测", "决策对比", "自定义"
    val orderIndex: Int,
    val isEnabled: Boolean = true,
    val timesUsed: Int = 0
)

data class AutomationRule(
    val silenceTimeoutSeconds: Int = 3,
    val maxRounds: Int = 5,
    val isAutoSendEnabled: Boolean = true,
    val sequencingMode: SequencingMode = SequencingMode.SEQUENTIAL,
    val soundVibrationEnabled: Boolean = true,
    val pauseOnStopKeywords: Boolean = true,
    val stopKeywords: String = "抱歉, 无法回答, 触发安全, I cannot, As an AI language model",
    val targetUrl: String = "https://lmarena.ai/"
)

enum class SequencingMode(val displayName: String, val description: String) {
    SEQUENTIAL("顺序递进", "按设置好的顺序逐条发送，到达末尾后停止"),
    LOOP("循环轮换", "按顺序发送，到达末尾后从第 1 条重新开始"),
    RANDOM("随机引导", "从启用的预设词库中随机抽取发送"),
    SMART_DEPTH("智能递进", "优先深度追问与反思，持续引导对话深入")
}

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val turnIndex: Int,
    val promptContent: String,
    val modelASnippet: String,
    val modelBSnippet: String,
    val charsDetected: Int,
    val status: String // "AUTO_SENT", "MANUAL_SENT", "SKIPPED", "MAX_ROUNDS_REACHED", "KEYWORD_TRIGGERED"
)

// === 对话管理实体 ===

@Entity(tableName = "conversations")
data class ConversationSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val mode: String = "ARENA_ONLINE",
    val modelAName: String = "Model A",
    val modelBName: String = "Model B",
    val totalTurns: Int = 0,
    val winnerVote: String = "NONE", // NONE, MODEL_A, MODEL_B, TIE, BOTH_BAD
    val notes: String = "",
    val isArchived: Boolean = false
)

@Entity(
    tableName = "conversation_turns",
    indices = [Index(value = ["sessionId"])]
)
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val turnIndex: Int,
    val userPrompt: String,
    val modelAResponse: String,
    val modelBResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAutoInjected: Boolean = false
)

data class SessionWithTurns(
    @Embedded val session: ConversationSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val turns: List<ConversationTurnEntity>
)

data class ArenaAccountInfo(
    val isLoggedIn: Boolean = false,
    val displayName: String = "Arena 访客 (未登录)",
    val email: String = "",
    val cookieSummary: String = "暂无活跃 Session Cookie",
    val hasActiveCookies: Boolean = false,
    val totalVotesSubmitted: Int = 0,
    val lastSyncTime: Long = 0L,
    val customAuthToken: String = ""
)

data class ModelIdentityInfo(
    val rawName: String = "??? 盲测中",
    val providerFamily: String = "待检测",
    val isRevealed: Boolean = false,
    val detectionSource: String = "等待投票揭晓或探针探测",
    val confidence: Float = 0.0f
) {
    companion object {
        fun resolveProvider(name: String): String {
            val lower = name.lowercase()
            return when {
                lower.contains("gpt") || lower.contains("o1") || lower.contains("o3") || lower.contains("openai") || lower.contains("chatgpt") -> "OpenAI"
                lower.contains("claude") || lower.contains("anthropic") || lower.contains("sonnet") || lower.contains("haiku") || lower.contains("opus") -> "Anthropic"
                lower.contains("gemini") || lower.contains("google") || lower.contains("gemma") -> "Google"
                lower.contains("deepseek") || lower.contains("r1") || lower.contains("深度求索") -> "DeepSeek"
                lower.contains("llama") || lower.contains("meta") -> "Meta"
                lower.contains("qwen") || lower.contains("alibaba") || lower.contains("通义") -> "Alibaba Qwen"
                lower.contains("mistral") || lower.contains("mixtral") -> "Mistral"
                lower.contains("command") || lower.contains("cohere") -> "Cohere"
                lower.contains("yi-") || lower.contains("01-ai") -> "01.AI"
                lower.contains("glm") || lower.contains("chatglm") || lower.contains("zhipu") -> "Zhipu GLM"
                else -> if (name.startsWith("Model") || name.contains("???")) "匿名盲测" else "其他模型"
            }
        }
    }
}

