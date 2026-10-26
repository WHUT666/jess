package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ModelIdentityInfo
import com.example.data.model.SequencingMode
import com.example.engine.ArenaJsBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ArenaPilot", appName)
    }

    @Test
    fun `verify sequencing modes are defined`() {
        assertEquals(4, SequencingMode.values().size)
        assertNotNull(SequencingMode.SEQUENTIAL.displayName)
    }

    @Test
    fun `verify bridge instantiates properly`() {
        val bridge = ArenaJsBridge()
        assertNotNull(bridge.events)
    }

    @Test
    fun `verify model provider resolution`() {
        assertEquals("OpenAI", ModelIdentityInfo.resolveProvider("gpt-4o-2024-08-06"))
        assertEquals("Anthropic", ModelIdentityInfo.resolveProvider("claude-3-5-sonnet"))
        assertEquals("Google", ModelIdentityInfo.resolveProvider("gemini-1.5-pro"))
        assertEquals("DeepSeek", ModelIdentityInfo.resolveProvider("deepseek-v2.5"))
        assertEquals("Meta", ModelIdentityInfo.resolveProvider("llama-3.1-70b-instruct"))
        assertEquals("Alibaba Qwen", ModelIdentityInfo.resolveProvider("qwen-2.5-72b"))
    }

    @Test
    fun `verify high tier model classification and low tier identification`() {
        // High tier models
        assertEquals(com.example.data.model.ModelTier.TIER_S_FLAGSHIP, com.example.engine.ArenaModelProbeEngine.classifyTier("gpt-4o-2024-08-06"))
        assertEquals(com.example.data.model.ModelTier.TIER_S_FLAGSHIP, com.example.engine.ArenaModelProbeEngine.classifyTier("o1-preview"))
        assertEquals(com.example.data.model.ModelTier.TIER_S_FLAGSHIP, com.example.engine.ArenaModelProbeEngine.classifyTier("claude-3-5-sonnet-20241022"))
        assertEquals(com.example.data.model.ModelTier.TIER_S_FLAGSHIP, com.example.engine.ArenaModelProbeEngine.classifyTier("deepseek-r1"))
        assertEquals(com.example.data.model.ModelTier.TIER_S_FLAGSHIP, com.example.engine.ArenaModelProbeEngine.classifyTier("gemini-1.5-pro"))

        // Low tier models (auto-discard targets)
        assertEquals(com.example.data.model.ModelTier.TIER_C_LOW, com.example.engine.ArenaModelProbeEngine.classifyTier("llama-3-8b-instruct"))
        assertEquals(com.example.data.model.ModelTier.TIER_C_LOW, com.example.engine.ArenaModelProbeEngine.classifyTier("qwen-2.5-7b"))
        assertEquals(com.example.data.model.ModelTier.TIER_C_LOW, com.example.engine.ArenaModelProbeEngine.classifyTier("mistral-7b"))

        // Reasoning snippet detection for anonymous model
        val thinkSnippet = "<think>\nThinking about the problem deeply...\n</think>\nHere is the answer."
        assertEquals(com.example.data.model.ModelTier.TIER_S_FLAGSHIP, com.example.engine.ArenaModelProbeEngine.classifyTier("Model A (盲测中)", thinkSnippet))
    }

    @Test
    fun `verify randomized canary fingerprint probe generation`() {
        val base = "请对比分布式共识算法"
        val (prompt, fp) = com.example.engine.ArenaModelProbeEngine.generateFingerprintedProbe(base, useRandomFingerprint = true)
        org.junit.Assert.assertTrue(prompt.contains(base))
        org.junit.Assert.assertTrue(prompt.contains("[PROBE-CANARY-FP: #$fp"))
        org.junit.Assert.assertEquals(8, fp.length)
    }

    @Test
    fun `verify account entity status defaults`() {
        val account = com.example.data.model.ManagedAccountEntity(
            email = "DaynaMorissettehnysz@outlook.com",
            password = "Ww@123456",
            label = "Outlook 1"
        )
        assertEquals("UNCHECKED", account.status)
        assertEquals(0L, account.lastCheckedAt)
    }
}
