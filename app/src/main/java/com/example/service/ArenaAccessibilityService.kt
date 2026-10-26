package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ArenaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ArenaAccService"
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _lastDetectedStatus = MutableStateFlow("服务未开启")
        val lastDetectedStatus: StateFlow<String> = _lastDetectedStatus.asStateFlow()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastObservedTextLength = 0
    private var isGenerating = false
    private var silenceRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true
        _lastDetectedStatus.value = "无障碍监控已连接，等待检测浏览器..."
        Log.d(TAG, "ArenaAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val rootNode = rootInActiveWindow ?: return

        try {
            // Check if page contains arena hints
            val isArenaPage = scanForKeywords(rootNode, listOf("arena", "lmsys", "chatbot arena", "model a", "model b"))
            if (!isArenaPage) return

            _lastDetectedStatus.value = "检测到 Arena.ai 活跃页面"

            // Look for stop indicators or generation text
            val hasStopBtn = scanForKeywords(rootNode, listOf("stop generating", "停止生成", "stop"))
            if (hasStopBtn) {
                isGenerating = true
                _lastDetectedStatus.value = "AI 正在生成回复..."
                silenceRunnable?.let { handler.removeCallbacks(it) }
            } else if (isGenerating) {
                // Was generating, now stop button is gone
                scheduleSilenceCheck()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting node", e)
        }
    }

    private fun scheduleSilenceCheck() {
        silenceRunnable?.let { handler.removeCallbacks(it) }
        silenceRunnable = Runnable {
            isGenerating = false
            _lastDetectedStatus.value = "AI 对话已停止，准备引导继续..."
            Log.d(TAG, "Generation stopped detected via Accessibility")
        }
        handler.postDelayed(silenceRunnable!!, 2500)
    }

    private fun scanForKeywords(node: AccessibilityNodeInfo?, keywords: List<String>): Boolean {
        if (node == null) return false
        val text = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
        val lower = text.lowercase()
        for (kw in keywords) {
            if (lower.contains(kw)) return true
        }
        for (i in 0 until node.childCount) {
            if (scanForKeywords(node.getChild(i), keywords)) return true
        }
        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "ArenaAccessibilityService interrupted")
        _lastDetectedStatus.value = "服务被中断"
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        _lastDetectedStatus.value = "服务已关闭"
    }
}
