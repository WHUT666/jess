package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PromptEntity
import com.example.engine.ArenaMonitorScript
import com.example.ui.theme.ModelABlue
import com.example.ui.theme.ModelBPink
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusSky
import com.example.viewmodel.ArenaPilotViewModel
import com.example.viewmodel.AutomationStatus

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArenaRunnerScreen(
    viewModel: ArenaPilotViewModel,
    navigateToUrl: String? = null,
    isImmersiveFullscreen: Boolean = false,
    onToggleImmersiveFullscreen: () -> Unit = {},
    onOpenAccountSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val countdown by viewModel.countdownSeconds.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val allPrompts by viewModel.allPrompts.collectAsState(initial = emptyList())
    val modelA by viewModel.detectedModelA.collectAsState()
    val modelB by viewModel.detectedModelB.collectAsState()
    val activeAccount by viewModel.activeManagedAccount.collectAsState(initial = null)

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableStateOf(0) }
    var isLoadingWeb by remember { mutableStateOf(true) }
    var showQuickInjectSheet by remember { mutableStateOf(false) }

    // HUD collapsed by default to avoid blocking arena web buttons and grid
    var isHudCollapsed by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(webViewInstance) {
        viewModel.jsEvaluator = { script ->
            webViewInstance?.post {
                webViewInstance?.evaluateJavascript(script, null)
            }
        }
        onDispose {
            viewModel.jsEvaluator = null
        }
    }

    LaunchedEffect(rules.targetUrl) {
        webViewInstance?.loadUrl(rules.targetUrl)
    }

    LaunchedEffect(navigateToUrl) {
        if (!navigateToUrl.isNullOrBlank()) {
            webViewInstance?.loadUrl(navigateToUrl)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Web View - Takes Full Screen Space
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("arena_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(viewModel.bridge, "ArenaBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoadingWeb = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoadingWeb = false
                            cookieManager.flush()
                            url?.let { viewModel.syncCookies(it) }

                            val script = ArenaMonitorScript.getInjectionScript(rules.silenceTimeoutSeconds * 1000)
                            view?.evaluateJavascript(script, null)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            webProgress = newProgress
                            if (newProgress >= 100) {
                                isLoadingWeb = false
                            }
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            transport?.webView = view
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }

                    loadUrl(rules.targetUrl)
                    webViewInstance = this
                }
            }
        )

        // Web Loading Progress Indicator
        if (isLoadingWeb && webProgress < 100) {
            LinearProgressIndicator(
                progress = { webProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // Top-Right Floating Controls (Quick Fullscreen & Refresh Toggle)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleImmersiveFullscreen,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_fullscreen_btn")
                    ) {
                        Icon(
                            imageVector = if (isImmersiveFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isImmersiveFullscreen) "退出全屏" else "全屏沉浸(不挡网页)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reload_webview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新页面",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Floating Bottom HUD Area - Designed to NOT obstruct Arena's grid or login buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Countdown Notification Card (if triggered)
            AnimatedVisibility(
                visible = status == AutomationStatus.COUNTDOWN,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .testTag("countdown_hud_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusAmber.copy(alpha = 0.96f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$countdown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "检测停顿！${countdown}s 后引导",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { viewModel.triggerImmediateSend() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("send_now_button")
                            ) {
                                Text("立即发送", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.cancelCountdown() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("cancel_countdown_button")
                            ) {
                                Text("取消", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Collapsible Floating Control Pill / Expanded Panel
            if (isHudCollapsed) {
                // MINIMAL NON-BLOCKING FLOATING PILL (Only ~40dp tall, leaves 100% of grid clickable)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collapsed_floating_pill"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Round & Status badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isHudCollapsed = false }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            AutomationStatus.STREAMING -> StatusSky
                                            AutomationStatus.COUNTDOWN -> StatusAmber
                                            AutomationStatus.SENDING -> MaterialTheme.colorScheme.primary
                                            else -> if (rules.isAutoSendEnabled) StatusEmerald else Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "第 $currentTurn 轮",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (telemetry.charsDetected > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${telemetry.charsDetected}字)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Right: Quick Actions (Auto-Fill Login, New Battle, Expand)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Quick Auto Fill Button
                            FilledTonalButton(
                                onClick = {
                                    val acc = activeAccount
                                    if (acc != null) {
                                        viewModel.autoFillCurrentPage(acc.email, acc.password)
                                    } else {
                                        onOpenAccountSheet()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("quick_fill_login_pill_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (activeAccount != null) "一键填密" else "选账号",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Quick Battle Trigger
                            FilledTonalButton(
                                onClick = { viewModel.startNewBattle() },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("quick_new_battle_pill_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("开启对战", fontSize = 10.sp)
                            }

                            // Expand / Collapse Chevron Button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { isHudCollapsed = false }
                                    .testTag("expand_hud_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "展开面板",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // EXPANDED CONTROL PANEL (Can be closed anytime)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("runner_control_panel"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Title bar with collapse button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "对战监控与引导面板",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "第 $currentTurn / ${rules.maxRounds} 轮",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { isHudCollapsed = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("collapse_hud_btn")
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("收起面板 (不挡网格)", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Model Identity Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(4.dp), color = ModelABlue.copy(alpha = 0.15f)) {
                                    Text(
                                        text = modelA.rawName,
                                        fontSize = 10.sp,
                                        color = ModelABlue,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text("⚔️", fontSize = 9.sp)
                                Surface(shape = RoundedCornerShape(4.dp), color = ModelBPink.copy(alpha = 0.15f)) {
                                    Text(
                                        text = modelB.rawName,
                                        fontSize = 10.sp,
                                        color = ModelBPink,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.probeModelIdentities() },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("探针识模", fontSize = 9.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Account Switch & Autofill
                            OutlinedButton(
                                onClick = onOpenAccountSheet,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(34.dp)
                                    .testTag("manage_accounts_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("账号登录", fontSize = 11.sp)
                            }

                            // Start New Battle
                            FilledTonalButton(
                                onClick = { viewModel.startNewBattle() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(34.dp)
                                    .testTag("runner_new_battle_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("开启对战", fontSize = 11.sp)
                            }

                            // Quick Prompt Injection
                            Button(
                                onClick = { showQuickInjectSheet = true },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(34.dp)
                                    .testTag("quick_inject_prompt_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("注入预设词", fontSize = 11.sp, maxLines = 1)
                            }

                            // Send Next Prompt
                            FilledTonalButton(
                                onClick = { viewModel.triggerImmediateSend() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(34.dp)
                                    .testTag("next_prompt_button")
                            ) {
                                Icon(imageVector = Icons.Default.Forward, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("发下条", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick Inject Prompt Bottom Sheet
        if (showQuickInjectSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showQuickInjectSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择要注入的引导提示词",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showQuickInjectSheet = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val enabledList = allPrompts.filter { it.isEnabled }
                    if (enabledList.isEmpty()) {
                        Text(
                            text = "当前没有启用的预设提示词，请在「提示词库」中启用或新增提示词。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(enabledList) { prompt ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.triggerImmediateSend(prompt.content)
                                            showQuickInjectSheet = false
                                        }
                                        .testTag("inject_prompt_${prompt.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = prompt.category,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = prompt.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = prompt.content,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "发送",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
