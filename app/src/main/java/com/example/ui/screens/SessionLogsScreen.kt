package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.SessionLogEntity
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
fun SessionLogsScreen(viewModel: ArenaPilotViewModel) {
    val context = LocalContext.current
    val logs by viewModel.allLogs.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "对战引导历史记录",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "记录自动发送的提示词与当时模型回复摘要",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (logs.isNotEmpty()) {
                Row {
                    OutlinedButton(
                        onClick = {
                            copyTranscriptMarkdown(context, logs, dateFormat)
                        },
                        modifier = Modifier.testTag("copy_transcript_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    TextButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "清空", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无对战引导记录",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "进入「对战监控」开始对话后将在此自动记录各轮引导",
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
                items(logs, key = { it.id }) { log ->
                    LogCard(log = log, dateFormat = dateFormat)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空会话记录？") },
            text = { Text("清空后将无法恢复当前的对战引导日志。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearLogs()
                    showClearDialog = false
                }) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun LogCard(log: SessionLogEntity, dateFormat: SimpleDateFormat) {
    val statusColor = when (log.status) {
        "AUTO_SENT" -> StatusEmerald
        "MANUAL_SENT" -> StatusSky
        "LIMIT_REACHED" -> StatusAmber
        "STOPPED_BY_KEYWORD" -> StatusRose
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when (log.status) {
        "AUTO_SENT" -> "自动注入"
        "MANUAL_SENT" -> "手动注入"
        "LIMIT_REACHED" -> "上限熔断"
        "STOPPED_BY_KEYWORD" -> "安全暂停"
        else -> log.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "第 ${log.turnIndex} 轮",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "引导词：${log.promptContent}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (log.modelASnippet.isNotBlank() || log.modelBSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (log.modelASnippet.isNotBlank()) {
                    Text(
                        text = "Model A: ${log.modelASnippet}",
                        fontSize = 11.sp,
                        color = ModelABlue,
                        maxLines = 2
                    )
                }
                if (log.modelBSnippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Model B: ${log.modelBSnippet}",
                        fontSize = 11.sp,
                        color = ModelBPink,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

private fun copyTranscriptMarkdown(context: Context, logs: List<SessionLogEntity>, dateFormat: SimpleDateFormat) {
    val sb = StringBuilder()
    sb.append("# ArenaPilot 对战记录导出\n\n")
    logs.reversed().forEach { log ->
        sb.append("## 第 ${log.turnIndex} 轮 (${dateFormat.format(Date(log.timestamp))})\n")
        sb.append("**发送提示词：** ${log.promptContent}\n\n")
        if (log.modelASnippet.isNotBlank()) {
            sb.append("**Model A 响应摘要：** ${log.modelASnippet}\n\n")
        }
        if (log.modelBSnippet.isNotBlank()) {
            sb.append("**Model B 响应摘要：** ${log.modelBSnippet}\n\n")
        }
        sb.append("---\n\n")
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("ArenaPilot Transcript", sb.toString())
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "已复制 Markdown 格式对战记录到剪贴板", Toast.LENGTH_SHORT).show()
}
