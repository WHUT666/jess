package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AccountStatus
import com.example.engine.AgentPagePolicy
import com.example.engine.ArenaAuthEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {
    @Test fun `app label remains ArenaPilot`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ArenaPilot", context.getString(R.string.app_name))
    }

    @Test fun `no sample accounts or fake battles are generated`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        assertEquals(0, db.accountDao().count())
        assertEquals(0, db.conversationDao().countSessions())
    }

    @Test fun `legacy direct-login probe cannot send credentials or claim unbanned`() = runBlocking {
        val result = ArenaAuthEngine.probeArenaLoginEndpoints("example@example.invalid", "not-a-real-password")
        assertEquals(AccountStatus.UNCHECKED, result.status)
        assertFalse(result.success)
        assertFalse(result.isBannedOnArena)
        assertEquals("", result.capturedCookies)
    }

    @Test fun `Agent route is the only resumable route and callbacks are sanitized`() {
        assertEquals("https://arena.ai/agent/123", AgentPagePolicy.resumableUrl("https://arena.ai/agent/123?code=do-not-store#x"))
        assertNull(AgentPagePolicy.resumableUrl("http://arena.ai/agent"))
        assertNull(AgentPagePolicy.resumableUrl("https://arena.ai.evil.example/agent"))
        assertNull(AgentPagePolicy.resumableUrl("https://arena.ai/battle"))
        assertNull(AgentPagePolicy.resumableUrl("https://user@arena.ai/agent"))
        assertTrue(AgentPagePolicy.isOfficialHost("accounts.arena.ai"))
        assertFalse(AgentPagePolicy.isOfficialHost("arena.ai.evil.example"))
    }

    @Test fun `403 is not interpreted as evidence of a ban`() {
        assertTrue(AgentPagePolicy.accessMessage(403).contains("不能据此判定封禁"))
        assertTrue(AgentPagePolicy.accessMessage(429).contains("频率限制"))
        assertTrue(AgentPagePolicy.AGENT_MODEL_STATUS.contains("未公开"))
    }
}
