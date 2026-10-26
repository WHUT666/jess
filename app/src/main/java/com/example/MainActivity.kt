package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.ArenaRepository
import com.example.engine.ArenaJsBridge
import com.example.ui.components.ArenaAccountSheet
import com.example.ui.components.ArenaBottomNav
import com.example.ui.components.ArenaTopBar
import com.example.ui.screens.ArenaRunnerScreen
import com.example.ui.screens.AutomationRulesScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.PromptLibraryScreen
import com.example.ui.screens.SessionLogsScreen
import com.example.ui.theme.ArenaPilotTheme
import com.example.viewmodel.ArenaPilotViewModel
import com.example.viewmodel.ArenaPilotViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val bridge = ArenaJsBridge()
    private val repository by lazy { ArenaRepository(applicationContext, lifecycleScope) }
    private val viewModel: ArenaPilotViewModel by viewModels {
        ArenaPilotViewModelFactory(repository, bridge, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ArenaPilotTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: ArenaPilotViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var pendingNavigateUrl by remember { mutableStateOf<String?>(null) }

    val status by viewModel.status.collectAsState()
    val countdown by viewModel.countdownSeconds.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val accountInfo by viewModel.accountInfo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var isImmersiveFullscreen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != 0 || isImmersiveFullscreen) {
        if (isImmersiveFullscreen) {
            isImmersiveFullscreen = false
        } else {
            selectedTab = 0
        }
    }

    val hideBars = isImmersiveFullscreen && selectedTab == 0

    Scaffold(
        contentWindowInsets = if (hideBars) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!hideBars) {
                ArenaTopBar(
                    status = status,
                    countdownSeconds = countdown,
                    isAutoSendEnabled = rules.isAutoSendEnabled,
                    isLoggedIn = accountInfo.isLoggedIn,
                    onToggleAutoSend = { viewModel.toggleAutoSend() },
                    onResetSession = { viewModel.resetSession() },
                    onOpenAccount = { showAccountSheet = true },
                    onToggleFullscreen = { isImmersiveFullscreen = !isImmersiveFullscreen }
                )
            }
        },
        bottomBar = {
            if (!hideBars) {
                ArenaBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ArenaRunnerScreen(
                    viewModel = viewModel,
                    navigateToUrl = pendingNavigateUrl,
                    isImmersiveFullscreen = isImmersiveFullscreen,
                    onToggleImmersiveFullscreen = { isImmersiveFullscreen = !isImmersiveFullscreen },
                    onOpenAccountSheet = { showAccountSheet = true }
                )
                1 -> ConversationsScreen(
                    viewModel = viewModel,
                    onNavigateToRunner = { selectedTab = 0 }
                )
                2 -> PromptLibraryScreen(viewModel)
                3 -> AutomationRulesScreen(viewModel)
                4 -> SessionLogsScreen(viewModel)
            }
        }
    }

    if (showAccountSheet) {
        ArenaAccountSheet(
            viewModel = viewModel,
            onDismiss = { showAccountSheet = false },
            onOpenArenaLoginUrl = {
                pendingNavigateUrl = "https://lmarena.ai/"
                selectedTab = 0
            }
        )
    }
}
