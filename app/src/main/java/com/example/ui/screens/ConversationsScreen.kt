package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationSessionEntity
import com.example.data.model.ConversationTurnEntity
import com.example.data.model.SessionWithTurns
import com.example.ui.theme.ModelABlue
import com.example.ui.theme.ModelBPink
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusSky
import com.example.viewmodel.ArenaPilotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationsScreen(
    viewModel: ArenaPilotViewModel,
    onNavigateToRunner: () -> Unit
) {
    val context = LocalContext.current
    val conversations by viewModel.allConversations.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var inspectingSessionId by remember { mutableStateOf<Long?>(null) }
    var showNewBattleDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    val filteredList = conversations.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewBattleDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_battle_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新建对战")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "对战与对话管理",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "已保存 ${conversations.size} 组评测对话",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showNewBattleDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("new_battle_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新建对战", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索对战主题或提示词...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(18.dp))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversation_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Conversations List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "暂无保存的对战对话" else "未找到匹配的对战记录",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "在对战监控中发起的问答将自动沉淀为会话记录",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { session ->
                        ConversationCard(
                            session = session,
                            dateFormat = dateFormat,
                            onClick = { inspectingSessionId = session.id },
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Inspect Turn-by-Turn Dialog
    inspectingSessionId?.let { sessId ->
        SessionDetailDialog(
            sessionId = sessId,
            viewModel = viewModel,
            onDismiss = { inspectingSessionId = null },
            onContinueBattle = {
                viewModel.selectSession(sessId)
                inspectingSessionId = null
                onNavigateToRunner()
            }
        )
    }

    // New Battle Dialog
    if (showNewBattleDialog) {
        var battleTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewBattleDialog = false },
            title = { Text("开启新一轮 Arena 对战") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "将清空当前界面的对话历史，开启全新的双模型评测会话：",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = battleTitle,
                        onValueChange = { battleTitle = it },
                        label = { Text("对战主题 / 备注 (可选)") },
                        placeholder = { Text("例如：微服务架构选型辩论") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.startNewBattle(battleTitle)
                    showNewBattleDialog = false
                    onNavigateToRunner()
                }) {
                    Text("立即开始")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBattleDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ConversationCard(
    session: ConversationSessionEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val voteBadge = when (session.winnerVote) {
        "MODEL_A" -> "Model A 胜" to ModelABlue
        "MODEL_B" -> "Model B 胜" to ModelBPink
        "TIE" -> "双方平手" to StatusSky
        "BOTH_BAD" -> "双方皆劣" to StatusRose
        else -> "未评判" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("session_card_${session.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (session.mode == "SANDBOX")
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (session.mode == "SANDBOX") "仿真沙盒" else "在线Arena",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (session.mode == "SANDBOX")
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = voteBadge.second.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = voteBadge.first,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = voteBadge.second,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(session.updatedAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.modelAName} ⚔️ ${session.modelBName} (共 ${session.totalTurns} 轮)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun SessionDetailDialog(
    sessionId: Long,
    viewModel: ArenaPilotViewModel,
    onDismiss: () -> Unit,
    onContinueBattle: () -> Unit
) {
    val context = LocalContext.current
    val sessionWithTurns by viewModel.getSessionWithTurnsFlow(sessionId).collectAsState(initial = null)

    var editingTitle by remember { mutableStateOf(false) }
    var newTitleInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sessionWithTurns?.session?.title ?: "对战详情",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    newTitleInput = sessionWithTurns?.session?.title ?: ""
                    editingTitle = true
                }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "编辑标题", modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val turns = sessionWithTurns?.turns ?: emptyList()
                if (turns.isEmpty()) {
                    Text("该会话暂无保存的轮次数据", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    turns.forEach { turn ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("第 ${turn.turnIndex} 轮问答", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    if (turn.isAutoInjected) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text("自动注入", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("提示词：${turn.userPrompt}", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Model A：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ModelABlue)
                                Text(turn.modelAResponse, fontSize = 11.sp, lineHeight = 16.sp)

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Model B：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ModelBPink)
                                Text(turn.modelBResponse, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                // Winner Voting Buttons
                Text("记录本次评测优胜方：", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.voteWinner(sessionId, "MODEL_A") },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("Model A 胜", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.voteWinner(sessionId, "MODEL_B") },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("Model B 胜", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.voteWinner(sessionId, "TIE") },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("平手", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onContinueBattle) {
                Text("在监控中继续此对话")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )

    if (editingTitle) {
        AlertDialog(
            onDismissRequest = { editingTitle = false },
            title = { Text("重命名对战标题") },
            text = {
                OutlinedTextField(
                    value = newTitleInput,
                    onValueChange = { newTitleInput = it },
                    label = { Text("会话标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTitleInput.isNotBlank()) {
                        viewModel.updateSessionTitle(sessionId, newTitleInput)
                    }
                    editingTitle = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTitle = false }) {
                    Text("取消")
                }
            }
        )
    }
}
