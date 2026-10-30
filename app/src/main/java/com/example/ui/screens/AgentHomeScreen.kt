package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.example.engine.AgentPagePolicy
import kotlinx.coroutines.launch

private fun openAgentInBrowser(context: Context, notify: (String) -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AgentPagePolicy.AGENT_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        })
    } catch (_: ActivityNotFoundException) {
        notify("没有可用的系统浏览器；请安装浏览器或使用内嵌页面。")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgentHomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var lastAgentUrl by rememberSaveable { mutableStateOf(AgentPagePolicy.AGENT_URL) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.isImeVisible
    val notify: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) showMore = false
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { if (!keyboardVisible) SnackbarHost(snackbar) },
        topBar = {
            if (!keyboardVisible) TopAppBar(
                title = { Text("Agent") },
                actions = {
                    if (selectedTab == 0) IconButton(onClick = {
                        webView?.loadUrl(AgentPagePolicy.AGENT_URL)
                    }, modifier = Modifier.testTag("official_login_button")) {
                        Icon(Icons.Default.Login, contentDescription = "官方登录或续用会话")
                    }
                    IconButton(onClick = { openAgentInBrowser(context, notify) },
                        modifier = Modifier.testTag("official_browser_button")) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "在系统浏览器打开 Agent")
                    }
                    Box {
                        IconButton(onClick = { showMore = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                            if (selectedTab == 0) DropdownMenuItem(
                                text = { Text("刷新页面") },
                                onClick = { showMore = false; webView?.reload() }
                            )
                            DropdownMenuItem(
                                text = { Text("账号与模型说明") },
                                onClick = { showMore = false; showInfo = true }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Hide the entire navigation chrome while typing: a collapsed HUD still covered the
            // web composer in the old implementation, particularly with edge-to-edge + IME.
            if (!keyboardVisible) NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                    label = { Text("Agent") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    label = { Text("提示词") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("旧版记录") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (selectedTab == 0) {
                AgentWebPage(
                    initialUrl = lastAgentUrl,
                    onUrlChanged = { url -> lastAgentUrl = url },
                    onWebViewChanged = { view -> webView = view },
                    onNotice = notify
                )
            } else if (selectedTab == 1) {
                ManualPromptScreen(onNotice = notify)
            } else {
                LegacyHistoryScreen()
            }
        }
    }

    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        title = { Text("官方会话与可核验信息") },
        text = {
            Box(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "点击登录会打开 Arena 官方 Agent 页面；首次 Google 或邮箱验证仍须本人完成。" +
                        "Google 登录若不支持内嵌页面，请用系统浏览器，并继续在浏览器中对话；两者会话不共享。\n\n" +
                        "本应用不保存或自动填写密码，不以 Cookie、HTTP 403 或网络故障判断账号封禁。" +
                        "如 Arena 明确提示账号被禁用，请以官网提示／客服确认为准。\n\n" +
                        AgentPagePolicy.AGENT_MODEL_STATUS +
                        "。旧版对战档案仅供本机查看，不能冒充新 Agent 会话。"
                )
            }
        },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("明白") } }
    )
}

@Composable
private fun AgentWebPage(
    initialUrl: String,
    onUrlChanged: (String) -> Unit,
    onWebViewChanged: (WebView?) -> Unit,
    onNotice: (String) -> Unit
) {
    val context = LocalContext.current
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val resultUris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileCallback?.onReceiveValue(resultUris)
        fileCallback = null
    }
    // Never install a JavaScript bridge, intercept login requests or read the page's cookies.
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag("agent_webview"),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true // required by the official Agent web app
                    domStorageEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    allowFileAccess = false
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
                }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val destination = request?.url ?: return false
                        if (destination.scheme != "https") return true
                        if (!AgentPagePolicy.isOfficialHost(destination.host)) {
                            if (destination.host.equals("accounts.google.com", ignoreCase = true)) {
                                onNotice("Google 登录不能在内嵌页面安全完成；请在系统浏览器打开 Agent 并在那里登录。")
                                openAgentInBrowser(context, onNotice)
                            } else {
                                onNotice("已在系统浏览器打开外部链接；内嵌页只允许 Arena 官方域名。")
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, destination).apply {
                                        addCategory(Intent.CATEGORY_BROWSABLE)
                                    })
                                } catch (_: ActivityNotFoundException) {
                                    onNotice("没有可用的系统浏览器。")
                                }
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        AgentPagePolicy.resumableUrl(url)?.let(onUrlChanged)
                        CookieManager.getInstance().flush()
                    }

                    override fun onReceivedHttpError(
                        view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?
                    ) {
                        if (request?.isForMainFrame == true && response != null) {
                            onNotice(AgentPagePolicy.accessMessage(response.statusCode))
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.cancel()
                        onNotice("安全连接证书校验失败；页面已停止加载。")
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        if (filePathCallback == null || fileChooserParams == null) return false
                        fileCallback?.onReceiveValue(null)
                        fileCallback = filePathCallback
                        return try {
                            filePicker.launch(fileChooserParams.createIntent())
                            true
                        } catch (_: Exception) {
                            fileCallback?.onReceiveValue(null)
                            fileCallback = null
                            onNotice("无法打开文件选择器，请在系统浏览器继续上传。")
                            true
                        }
                    }
                }
                loadUrl(AgentPagePolicy.resumableUrl(initialUrl) ?: AgentPagePolicy.AGENT_URL)
                onWebViewChanged(this)
            }
        },
        onRelease = { view ->
            fileCallback?.onReceiveValue(null)
            fileCallback = null
            onWebViewChanged(null)
            view.stopLoading()
            view.webChromeClient = null
            view.webViewClient = WebViewClient()
            view.destroy()
        }
    )
}
