package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.db.AppDatabase

/** Existing records remain on-device. No site scraping or new fake sample rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyHistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context, scope).conversationDao() }
    val flow = remember(dao) { dao.getAllSessionsFlow() }
    val sessions by flow.collectAsState(initial = emptyList())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedFlow = remember(dao, selectedId) {
        selectedId?.let(dao::getSessionWithTurnsFlow)
    }
    val selected = selectedFlow?.collectAsState(initial = null)?.value

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("旧版对战记录（本地只读）")
        Text("这里仅展示升级前已保存的记录；Agent 对话由 Arena 官方网站管理，旧型号标签不作身份认证。")
        if (selectedId != null) {
            TextButton(onClick = { selectedId = null }) { Text("返回列表") }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selectedId == null) {
                if (sessions.isEmpty()) item { Text("没有旧记录。不会为新用户生成演示对话。") }
                items(sessions, key = { it.id }) { session ->
                    Card(onClick = { selectedId = session.id }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(session.title)
                            Text("${session.totalTurns} 轮 · 历史标签（未核验）：${session.modelAName} / ${session.modelBName}")
                        }
                    }
                }
            } else {
                item {
                    Text(selected?.session?.title ?: "读取旧记录中…")
                    Text("以下内容来自旧版本地数据库；不作为 Agent 模型身份依据。")
                }
                items(selected?.turns.orEmpty(), key = { it.id }) { turn ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("问题：${turn.userPrompt}")
                            Text("历史回复 A：${turn.modelAResponse}")
                            Text("历史回复 B：${turn.modelBResponse}")
                        }
                    }
                }
            }
        }
    }
}
