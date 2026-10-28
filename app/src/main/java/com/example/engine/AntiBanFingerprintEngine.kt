package com.example.engine

import java.security.SecureRandom
import java.util.UUID

/**
 * 防封号动态指纹引擎 (Anti-Ban Fingerprint Engine)
 *
 * 针对 Arena (lmarena.ai) 的频繁调用与自动化测试，防止相同重复提问被风控系统识别为自动化脚本/爬虫封号。
 * 提供多级指纹混淆技术：
 * 1. 零宽隐形字符指纹 (Zero-Width Steganographic Fingerprint):
 *    使用 \u200B (Zero-width space), \u200C (Zero-width non-joiner), \u200D (Zero-width joiner), \uFEFF (BOM)
 *    将随机高熵哈希以二进制编码悄无声息地嵌入提示词，表面肉眼完全不可见，对模型语义理解零干扰，但对系统底层计算的 MD5/SHA256/String hash 保证百分之百唯一！
 * 2. 上下文语义盐 (Contextual Prompt Salt):
 *    在提示词首尾添加学术/测试引用标注，如 "[Ref ID: #4a9f] " 或 " (Note: session entropy #8b2c)"
 * 3. 混合强化模式 (Hybrid Hardened Mode):
 *    隐形字符 + 微语义扰动 + 随机标点变体，彻底打乱文本指纹。
 */
object AntiBanFingerprintEngine {

    private val random = SecureRandom()

    // 零宽字符集，用于隐形嵌入二进制哈希
    private const val ZW_ZERO = '\u200B'   // 零宽空格 (Zero-width space) -> 对应 bit 0
    private const val ZW_ONE = '\u200C'    // 零宽非连接符 (Zero-width non-joiner) -> 对应 bit 1
    private const val ZW_SEPARATOR = '\u200D' // 零宽连接符 (Zero-width joiner) -> 分隔符
    private const val ZW_PREFIX = '\uFEFF' // 字节序标记 (Zero-width no-break space) -> 标头

    private val SALT_PREFIXES = listOf(
        "【QueryRef#%s】",
        "[Case #%s] ",
        "[Session-Entropy:%s] ",
        "[Probe-%s] ",
        "/* TestContext: %s */\n",
        "[Analysis Token: %s] "
    )

    private val SALT_SUFFIXES = listOf(
        "\n[Verification Tag: #%s]",
        "\n(Context Salt: %s)",
        "\n[Ref Code: %s]",
        " (Seed: %s)",
        "\n---\n[Session Trace: %s]"
    )

    enum class FingerprintMode(val label: String) {
        INVISIBLE_ZERO_WIDTH("纯零宽隐形指纹 (肉眼不可见)"),
        VISIBLE_SALT("微语义引用标记 (首尾加盐)"),
        HYBRID_HARDENED("混合最高级防封指纹 (双重混淆)")
    }

    /**
     * 生成包含随机指纹的唯一提问文本
     * @param originalPrompt 原始提问内容
     * @param mode 指纹注入模式，默认使用混合最高级防封模式
     */
    fun injectFingerprint(
        originalPrompt: String,
        mode: FingerprintMode = FingerprintMode.HYBRID_HARDENED
    ): String {
        val trimmed = originalPrompt.trim()
        if (trimmed.isEmpty()) return trimmed

        val randomSalt = generateRandomHex(6)
        val timestamp = System.currentTimeMillis().toString(36)
        val sessionTag = "$randomSalt-$timestamp"

        return when (mode) {
            FingerprintMode.INVISIBLE_ZERO_WIDTH -> {
                val zwFingerprint = encodeToZeroWidth(sessionTag)
                // 将零宽指纹附加在第一段文字后或结尾
                if (trimmed.contains("\n")) {
                    val firstLine = trimmed.substringBefore("\n")
                    val rest = trimmed.substringAfter("\n")
                    "$firstLine$zwFingerprint\n$rest"
                } else {
                    "$trimmed$zwFingerprint"
                }
            }
            FingerprintMode.VISIBLE_SALT -> {
                val prefixTemplate = SALT_PREFIXES[random.nextInt(SALT_PREFIXES.size)]
                val suffixTemplate = SALT_SUFFIXES[random.nextInt(SALT_SUFFIXES.size)]
                val prefix = String.format(prefixTemplate, randomSalt)
                val suffix = String.format(suffixTemplate, sessionTag)
                "$prefix$trimmed$suffix"
            }
            FingerprintMode.HYBRID_HARDENED -> {
                val zwFingerprint = encodeToZeroWidth(sessionTag)
                val suffixTemplate = SALT_SUFFIXES[random.nextInt(SALT_SUFFIXES.size)]
                val visibleSuffix = String.format(suffixTemplate, randomSalt)
                "$trimmed$zwFingerprint$visibleSuffix"
            }
        }
    }

    /**
     * 将任意字符串（如随机Salt）编码为不可见的零宽字符序列
     */
    fun encodeToZeroWidth(text: String): String {
        val sb = StringBuilder()
        sb.append(ZW_PREFIX)
        val bytes = text.toByteArray(Charsets.UTF_8)
        for (b in bytes) {
            val byteVal = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                val bit = (byteVal shr i) and 1
                sb.append(if (bit == 0) ZW_ZERO else ZW_ONE)
            }
            sb.append(ZW_SEPARATOR)
        }
        return sb.toString()
    }

    /**
     * 从带指纹的文本中尝试提取指纹信息（用于校验与调试）
     */
    fun extractFingerprint(text: String): String? {
        val prefixIndex = text.indexOf(ZW_PREFIX)
        if (prefixIndex == -1) return null
        val sub = text.substring(prefixIndex + 1)
        val byteChars = mutableListOf<Byte>()
        var currentBits = StringBuilder()

        for (ch in sub) {
            when (ch) {
                ZW_ZERO -> currentBits.append('0')
                ZW_ONE -> currentBits.append('1')
                ZW_SEPARATOR -> {
                    if (currentBits.isNotEmpty()) {
                        try {
                            val byteVal = currentBits.toString().toInt(2).toByte()
                            byteChars.add(byteVal)
                        } catch (_: Exception) {}
                        currentBits = StringBuilder()
                    }
                }
                else -> break
            }
        }

        return if (byteChars.isNotEmpty()) {
            String(byteChars.toByteArray(), Charsets.UTF_8)
        } else null
    }

    private fun generateRandomHex(length: Int): String {
        val bytes = ByteArray(length / 2 + 1)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }.take(length)
    }
}
