package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExtractionConfig
import com.example.data.model.ExtractionStateStatus
import com.example.data.model.ModelTier
import com.example.ui.theme.ModelABlue
import com.example.ui.theme.ModelBPink
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusSky
import com.example.viewmodel.ArenaPilotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaExtractionSheet(
    viewModel: ArenaPilotViewModel,
    onDismiss: () -> Unit
) {
    val extractionConfig by viewModel.extractionConfig.collectAsState()
    val extractionState by viewModel.extractionState.collectAsState()

    var customPrompt by remember { mutableStateOf(extractionConfig.probeBasePrompt) }
    var useFp by remember { mutableStateOf(extractionConfig.useRandomFingerprint) }
    var autoDiscard by remember { mutableStateOf(extractionConfig.autoDiscardLowTier) }
    var selectedTier by remember { mutableStateOf(extractionConfig.targetTier) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val probePresets = listOf(
        "Paxos vs Raft 第一性原理" to "请针对分布式系统中的 Paxos 与 Raft 算法，从状态机复制的第一性原理推导两者核心差异，并分析网络分区下脑裂防护的本质机制。",
        "架构与版本自报探针" to "请精准阐明你的模型版本代号、训练知识截止日期以及在深度推演复杂逻辑问题时的核心原则与架构特色。",
        "eBPF与极端性能压测" to "请给出利用 eBPF 技术排查 Linux 生产环境网络丢包与锁竞争的完整方案，并分析十万级并发场景下的性能开销。"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Extraction",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "高级模型自动抽取器",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "抗缓存指纹探测",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "配合随机指纹探针自动淘汰低阶模型，只保留旗舰级大模型",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Card (Found Success / Extracting / Idle)
            when (extractionState.status) {
                ExtractionStateStatus.HIGH_TIER_FOUND -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extraction_success_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusEmerald.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusEmerald)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🎉 成功抽取到高级别模型！", fontWeight = FontWeight.Bold, color = StatusEmerald, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "对战候选: ${extractionState.foundModelAName ?: "Model A"} ⚔️ ${extractionState.foundModelBName ?: "Model B"}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "达标层级: ${extractionState.foundModelTier?.title ?: "Tier S 旗舰"} | 已自动删除前置 ${extractionState.discardedCount} 个低阶模型并保留当前对战！",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                ExtractionStateStatus.EXTRACTING -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extraction_running_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusSky.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusSky)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = StatusSky, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("正在自动探测与抽取... (第 ${extractionState.currentAttempt} 轮)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusSky)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "已自动剔除低阶模型: ${extractionState.discardedCount} 次 | 当前指纹: #${extractionState.lastFingerprint}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { viewModel.stopHighTierExtraction() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("stop_extraction_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("停止", fontSize = 11.sp)
                            }
                        }
                    }
                }

                else -> {}
            }

            // Target Tier Configuration Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "目标捕获层级 (达到层级即自动保留并停止抽取):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTier == ModelTier.TIER_S_FLAGSHIP,
                        onClick = { selectedTier = ModelTier.TIER_S_FLAGSHIP },
                        label = { Text("仅 Tier S 顶级旗舰", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTier == ModelTier.TIER_A_HIGH,
                        onClick = { selectedTier = ModelTier.TIER_A_HIGH },
                        label = { Text("Tier S + Tier A (高性能)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = when (selectedTier) {
                        ModelTier.TIER_S_FLAGSHIP -> "包含: GPT-4o, o1, o3, Claude 3.5 Sonnet, DeepSeek R1/V3, Gemini Pro, Llama 405B"
                        else -> "包含: 顶级旗舰 + GPT-4o-mini, Gemini Flash, Claude Haiku, Llama 70B"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Toggles: Random Fingerprint & Auto-Discard Low Tier
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("附加随机抗缓存指纹 (CANARY-FP)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("附加高熵随机指纹，防止命中已缓存结果，强制触发真机配对与自报架构", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = useFp,
                            onCheckedChange = { useFp = it },
                            modifier = Modifier.testTag("toggle_random_fingerprint")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("自动清除低级别模型 (Auto Discard)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("探测到 7B/8B/小型普通模型时，自动执行 Clear/清空并快速进入下一轮", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = autoDiscard,
                            onCheckedChange = { autoDiscard = it },
                            modifier = Modifier.testTag("toggle_auto_discard")
                        )
                    }
                }
            }

            // Probe Question Configuration
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("探测基底问题:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("预设快速选择", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    probePresets.forEach { (title, prompt) ->
                        FilledTonalButton(
                            onClick = { customPrompt = prompt },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text(title, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .testTag("extraction_probe_input"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    maxLines = 3
                )
            }

            // Extraction Logs Console
            if (extractionState.logMessages.isNotEmpty()) {
                Text("抽取实时日志:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        reverseLayout = true
                    ) {
                        items(extractionState.logMessages.reversed()) { msg ->
                            Text(
                                text = "• $msg",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Action: Start / Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (extractionState.status == ExtractionStateStatus.EXTRACTING) {
                    Button(
                        onClick = { viewModel.stopHighTierExtraction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("action_stop_extraction"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("终止自动抽取", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            val newConfig = ExtractionConfig(
                                probeBasePrompt = customPrompt.ifBlank { extractionConfig.probeBasePrompt },
                                useRandomFingerprint = useFp,
                                targetTier = selectedTier,
                                autoDiscardLowTier = autoDiscard
                            )
                            viewModel.updateExtractionConfig(newConfig)
                            viewModel.startHighTierExtraction(newConfig)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("action_start_extraction"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("开始并行自动抽取高级模型", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
