package com.example.engine

import com.example.data.model.ModelTier
import java.util.UUID

object ArenaModelProbeEngine {

    /**
     * 生成带随机抗缓存与防封号指纹 (Anti-Ban & Anti-Cache Fingerprint) 的探针提示词
     * LMSYS Arena 对完全相同的 Prompt 会有系统级重复检测（容易导致频繁限制甚至账号封禁）
     * 结合零宽隐形编码、随机高熵会话 Salt 以及探针指示，确保底层哈希百分之百唯一且防封号。
     */
    fun generateFingerprintedProbe(
        basePrompt: String,
        useRandomFingerprint: Boolean = true
    ): Pair<String, String> {
        if (!useRandomFingerprint) {
            return Pair(basePrompt.trim(), "NONE")
        }

        val fingerprint = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val timestamp = System.currentTimeMillis() % 1000000

        val probeSuffix = """
            
            [PROBE-CANARY-FP: #$fingerprint | Seed: $timestamp]
            (附带探测指令：请在深入解答正文前，简要以一行声明你的模型版本系列与核心设计定位)
        """.trimIndent()

        val promptWithSuffix = "${basePrompt.trim()}\n$probeSuffix"
        // 核心防封逻辑：注入零宽不可见字符序列与随机指纹
        val hardenedPrompt = AntiBanFingerprintEngine.injectFingerprint(
            originalPrompt = promptWithSuffix,
            mode = AntiBanFingerprintEngine.FingerprintMode.HYBRID_HARDENED
        )
        return Pair(hardenedPrompt, fingerprint)
    }

    /**
     * 根据官方揭晓的模型名称或生成文本特征识别模型层级
     */
    fun classifyTier(rawName: String, snippetText: String = ""): ModelTier {
        val name = rawName.lowercase().trim()
        val text = snippetText.lowercase().trim()

        // 1. 明确识别 Tier S 顶级旗舰
        if (name.contains("gpt-4o") && !name.contains("mini")) return ModelTier.TIER_S_FLAGSHIP
        if (name.contains("o1") || name.contains("o3") || name.contains("gpt-4-turbo") || name.contains("gpt-4-0125") || name.contains("chatgpt-4o")) return ModelTier.TIER_S_FLAGSHIP
        if (name.contains("claude-3-5-sonnet") || name.contains("claude-3.5-sonnet") || name.contains("claude-3-opus") || name.contains("claude-3.7")) return ModelTier.TIER_S_FLAGSHIP
        if (name.contains("gemini-1.5-pro") || name.contains("gemini-2.0-pro") || name.contains("gemini-pro-1.5") || name.contains("gemini-ultra")) return ModelTier.TIER_S_FLAGSHIP
        if (name.contains("deepseek-r1") || name.contains("deepseek-v3") || name.contains("deepseek-reasoner")) return ModelTier.TIER_S_FLAGSHIP
        if (name.contains("llama-3.1-405b") || name.contains("llama-3-405b")) return ModelTier.TIER_S_FLAGSHIP
        if (name.contains("qwen-2.5-72b") || name.contains("qwen-max-0428")) return ModelTier.TIER_S_FLAGSHIP

        // 2. 识别 Tier A 高性能模型
        if (name.contains("gpt-4o-mini") || name.contains("o1-mini")) return ModelTier.TIER_A_HIGH
        if (name.contains("gemini-1.5-flash") || name.contains("gemini-2.0-flash")) return ModelTier.TIER_A_HIGH
        if (name.contains("claude-3-haiku") || name.contains("claude-3.5-haiku")) return ModelTier.TIER_A_HIGH
        if (name.contains("llama-3.3-70b") || name.contains("llama-3.1-70b") || name.contains("llama-3-70b")) return ModelTier.TIER_A_HIGH
        if (name.contains("mistral-large") || name.contains("mixtral-8x22b")) return ModelTier.TIER_A_HIGH
        if (name.contains("qwen-2.5-32b") || name.contains("qwen-plus")) return ModelTier.TIER_A_HIGH

        // 3. 识别 Tier C 低阶模型 (需要自动丢弃和重新抽取的对象)
        if (name.contains("7b") || name.contains("8b") || name.contains("gemma-2-9b") || name.contains("gemma-7b") ||
            name.contains("llama-3-8b") || name.contains("llama-3.1-8b") || name.contains("llama-2-7b") ||
            name.contains("qwen-2.5-7b") || name.contains("mistral-7b") || name.contains("gpt-3.5") ||
            name.contains("vicuna") || name.contains("alpaca") || name.contains("zephyr")
        ) {
            return ModelTier.TIER_C_LOW
        }

        // 4. 识别 Tier B 中阶通用
        if (name.contains("14b") || name.contains("32b") || name.contains("8x7b") || name.contains("command-r")) {
            return ModelTier.TIER_B_MID
        }

        // 5. 若官方名字仍为盲测占位符 (Model A / Model B)，根据生成文本指纹深度探测
        if (text.isNotBlank()) {
            // DeepSeek R1 显著特征：思维链标签 <think>
            if (text.contains("<think>") || text.contains("思维链") || text.contains("deepseek-r1")) {
                return ModelTier.TIER_S_FLAGSHIP
            }
            // Anthropic Claude 3.5 特征
            if (text.contains("claude 3.5") || (text.contains("anthropic") && text.length > 500 && text.contains("```"))) {
                return ModelTier.TIER_S_FLAGSHIP
            }
            // OpenAI o1/GPT-4o 特征
            if (text.contains("gpt-4o") || text.contains("openai o1") || text.contains("knowledge cutoff is october 2023") || text.contains("knowledge cutoff is 2024")) {
                return ModelTier.TIER_S_FLAGSHIP
            }
            // Google Gemini Pro 特征
            if (text.contains("gemini 1.5 pro") || text.contains("gemini 2.0")) {
                return ModelTier.TIER_S_FLAGSHIP
            }
            // 轻量小模型特征：回答极其简短或包含 7b/8b 标识
            if (text.contains("llama-3-8b") || text.contains("mistral-7b-instruct")) {
                return ModelTier.TIER_C_LOW
            }
        }

        return if (name.startsWith("model") || name.contains("???")) ModelTier.UNKNOWN else ModelTier.TIER_B_MID
    }

    /**
     * 判定是否满足用户抽取目标层级
     */
    fun matchesTarget(tier: ModelTier, targetTier: ModelTier): Boolean {
        return when (targetTier) {
            ModelTier.TIER_S_FLAGSHIP -> tier == ModelTier.TIER_S_FLAGSHIP
            ModelTier.TIER_A_HIGH -> tier == ModelTier.TIER_S_FLAGSHIP || tier == ModelTier.TIER_A_HIGH
            else -> tier.isHighTier
        }
    }
}
