package com.example.engine

object ArenaSandboxHtml {
    fun getHtml(): String {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>LMSYS Chatbot Arena - 仿真对战沙盒</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background-color: #0b0f19; color: #f1f5f9; min-height: 100vh; display: flex; flex-direction: column; }
        header { background: #111827; border-bottom: 1px solid #1f2937; padding: 12px 16px; display: flex; align-items: center; justify-content: space-between; }
        .logo-box { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 15px; color: #38bdf8; }
        .badge { background: rgba(56, 189, 248, 0.15); color: #38bdf8; border: 1px solid #38bdf8; font-size: 11px; padding: 2px 8px; border-radius: 12px; }
        .main-chat { flex: 1; padding: 12px; overflow-y: auto; display: flex; flex-direction: column; gap: 14px; }
        
        .turn-banner { text-align: center; margin: 4px 0; }
        .turn-chip { display: inline-block; background: #1e293b; color: #94a3b8; font-size: 11px; padding: 3px 12px; border-radius: 10px; border: 1px solid #334155; }
        
        .user-bubble { background: #1e3a8a; color: #e0f2fe; padding: 10px 14px; border-radius: 16px 16px 2px 16px; align-self: flex-end; max-width: 90%; font-size: 14px; line-height: 1.5; border: 1px solid #2563eb; }
        
        .arena-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 4px; }
        @media (max-width: 500px) {
            .arena-grid { grid-template-columns: 1fr; }
        }
        
        .model-card { background: #111827; border: 1px solid #1f2937; border-radius: 12px; padding: 12px; display: flex; flex-direction: column; }
        .model-card.model-a { border-top: 3px solid #3b82f6; }
        .model-card.model-b { border-top: 3px solid #ec4899; }
        
        .model-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-size: 12px; font-weight: 600; }
        .model-head.a-head { color: #60a5fa; }
        .model-head.b-head { color: #f472b6; }
        
        .model-body { font-size: 13px; line-height: 1.5; color: #cbd5e1; min-height: 48px; white-space: pre-wrap; word-break: break-word; }
        .model-body .cursor { display: inline-block; width: 6px; height: 14px; background: #38bdf8; animation: blink 1s infinite; vertical-align: middle; margin-left: 2px; }
        
        @keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

        .vote-bar { display: flex; gap: 6px; margin-top: 8px; padding-top: 6px; border-top: 1px dashed #1f2937; }
        .vote-btn { flex: 1; padding: 4px 6px; font-size: 11px; background: #1e293b; color: #94a3b8; border: 1px solid #334155; border-radius: 6px; cursor: pointer; text-align: center; }
        .vote-btn:hover { background: #334155; color: #f8fafc; }

        .starter-bar { padding: 6px 12px; background: #0f172a; display: flex; gap: 6px; overflow-x: auto; white-space: nowrap; border-top: 1px solid #1e293b; }
        .starter-chip { background: #1e293b; color: #94a3b8; font-size: 11px; padding: 4px 10px; border-radius: 14px; border: 1px solid #334155; cursor: pointer; flex-shrink: 0; }
        
        .input-bar { background: #111827; border-top: 1px solid #1f2937; padding: 10px 12px; display: flex; gap: 8px; align-items: center; }
        .input-bar textarea { flex: 1; height: 38px; background: #1e293b; border: 1px solid #334155; border-radius: 8px; color: #f8fafc; padding: 8px 10px; font-size: 13px; resize: none; outline: none; }
        .input-bar textarea:focus { border-color: #38bdf8; }
        .btn-send { background: #0284c7; color: white; border: none; border-radius: 8px; padding: 0 16px; height: 38px; font-weight: 600; font-size: 13px; cursor: pointer; display: flex; align-items: center; gap: 4px; }
        .btn-send:disabled { background: #334155; color: #64748b; cursor: not-allowed; }
        .btn-stop { background: #ef4444; color: white; border: none; border-radius: 8px; padding: 0 14px; height: 38px; font-weight: 600; font-size: 13px; cursor: pointer; display: none; }
    </style>
</head>
<body>
    <header>
        <div class="logo-box">
            <span>⚡ LMSYS Arena</span>
            <span class="badge">对战仿真沙盒</span>
        </div>
        <div style="font-size: 11px; color: #94a3b8;" id="status-indicator">就绪</div>
    </header>

    <div class="main-chat" id="chatContainer">
        <div class="turn-banner" id="initBanner">
            <span class="turn-chip">对战开始：请输入一个初始话题或点击下方快捷话题</span>
        </div>
    </div>

    <div class="starter-bar" id="starterBar">
        <div class="starter-chip" onclick="quickStart('请分析分布式缓存与关系型数据库的双写一致性解决方案')">⚡ 缓存双写一致性</div>
        <div class="starter-chip" onclick="quickStart('请对比微服务架构与现代模块化单体架构的选型考量')">🏛️ 微服务 vs 单体架构</div>
        <div class="starter-chip" onclick="quickStart('量子计算将如何重塑现代RSA与ECC公钥密码学体系？')">🔐 量子计算与密码学</div>
    </div>

    <div class="input-bar">
        <textarea id="promptInput" placeholder="输入对话内容，或等待自动提示词注入..."></textarea>
        <button id="sendBtn" class="btn-send" onclick="handleUserSubmit()">发送</button>
        <button id="stopBtn" class="btn-stop" onclick="stopGeneration()">停止</button>
    </div>

    <script>
        var currentTurn = 0;
        var isStreaming = false;
        var streamTimerA = null;
        var streamTimerB = null;
        var lastUserPrompt = '';

        var knowledgeBase = {
            round1A: "分布式缓存与关系型数据库双写一致性的主流策略包括：\n1. 延时双删方案：先删缓存，写入数据库，休眠数百毫秒后再次删除缓存，防止读取脏数据覆盖。\n2. 订阅Binlog异步更新（推荐）：通过Canal或Debezium监听MySQL的Row格式binlog，解耦写入与缓存失效逻辑，天然具备最终一致性与重试补偿。",
            round1B: "保证双写一致性必须依据CAP与业务容忍度权衡：\n• 强一致性需求：使用分布式读写锁（如Redisson RReadWriteLock）或两阶段提交（2PC），但吞吐量下降明显。\n• 最终一致性：先写DB，再基于RocketMQ事务消息进行缓存更新/删除，失败自动死信队列重试。核心原则是『只删缓存，不更新缓存』。",
            critiqueA: "【模型A辩驳】：\n模型B提出的分布式锁严重违背了引入缓存提高性能的初衷。在峰值10万QPS场景下，分布式锁会导致严重的线程排队与超时雪崩！而Binlog异步方案仅需几毫秒同步延迟即可满足99.9%的业务场景，兼顾高吞吐与容灾。",
            critiqueB: "【模型B反思与辩驳】：\n模型A忽略了Binlog订阅方案的架构复杂度与高额运维成本（需搭建Canal集群、Kafka中间件）。对于绝大多数中小团队，引入该链路意味着运维故障面成倍扩大。延时双删中的『休眠时间』是经验玄学，在主从复制延迟波动时必然失效！",
            deepA: "【第一性原理深层推导】：\n双写一致性的本质是『分布式节点状态机的偏序同步问题』。从理论上讲，只要存在两个独立的网络实体，若不借由共识协议（Paxos/Raft），就只能追求最终一致。因此设计重点应转移至：幂等版本号校验（Version Header）与客户端脏读自愈机制。",
            deepB: "【模型B工程实现】：\n同意模型A关于状态机的理论拆解。在极端边界条件下，我们通常在Redis中存放数据指纹（MD5/Timestamp），写入时采用Lua脚本原子校验：若Redis中的版本晚于DB写入时间戳则拒绝覆盖，彻底消除了并发网络乱序导致的时序翻转问题。"
        };

        function quickStart(text) {
            document.getElementById('promptInput').value = text;
            handleUserSubmit();
        }

        function handleUserSubmit() {
            var input = document.getElementById('promptInput');
            var text = input.value.trim();
            if (!text || isStreaming) return;

            currentTurn++;
            lastUserPrompt = text;
            input.value = '';
            
            document.getElementById('starterBar').style.display = 'none';

            var chat = document.getElementById('chatContainer');
            var userDiv = document.createElement('div');
            userDiv.className = 'user-bubble';
            userDiv.innerText = text;
            chat.appendChild(userDiv);

            var grid = document.createElement('div');
            grid.className = 'arena-grid';
            grid.id = 'grid-turn-' + currentTurn;

            grid.innerHTML = `
                <div class="model-card model-a">
                    <div class="model-head a-head"><span>🤖 Model A (Gemini 1.5 Pro)</span><span id="speedA-` + currentTurn + `">生成中</span></div>
                    <div class="model-body" id="bodyA-` + currentTurn + `"><span class="cursor"></span></div>
                </div>
                <div class="model-card model-b">
                    <div class="model-head b-head"><span>🔮 Model B (Claude 3.5 Sonnet)</span><span id="speedB-` + currentTurn + `">生成中</span></div>
                    <div class="model-body" id="bodyB-` + currentTurn + `"><span class="cursor"></span></div>
                </div>
            `;
            chat.appendChild(grid);
            chat.scrollTop = chat.scrollHeight;

            var targetA = (currentTurn === 1) ? knowledgeBase.round1A : 
                          (currentTurn % 2 === 0) ? knowledgeBase.critiqueA : knowledgeBase.deepA;
            var targetB = (currentTurn === 1) ? knowledgeBase.round1B : 
                          (currentTurn % 2 === 0) ? knowledgeBase.critiqueB : knowledgeBase.deepB;

            startStreaming(targetA, targetB, currentTurn);
        }

        function startStreaming(textA, textB, turn) {
            isStreaming = true;
            document.getElementById('sendBtn').disabled = true;
            document.getElementById('stopBtn').style.display = 'block';
            document.getElementById('status-indicator').innerText = '⚡ 模型正在生成回复...';

            var elA = document.getElementById('bodyA-' + turn);
            var elB = document.getElementById('bodyB-' + turn);
            var idxA = 0;
            var idxB = 0;
            var doneA = false;
            var doneB = false;

            if (window.ArenaBridge && window.ArenaBridge.onStreamProgress) {
                window.ArenaBridge.onStreamProgress(0, true, "开始生成...", "");
            }

            streamTimerA = setInterval(function() {
                if (idxA < textA.length) {
                    var chunkSize = Math.floor(Math.random() * 3) + 2;
                    idxA = Math.min(textA.length, idxA + chunkSize);
                    elA.innerHTML = escapeHtml(textA.substring(0, idxA)) + '<span class="cursor"></span>';
                    notifyBridgeProgress();
                } else {
                    doneA = true;
                    clearInterval(streamTimerA);
                    elA.innerHTML = escapeHtml(textA);
                    checkBothFinished(doneA, doneB, textA, textB);
                }
            }, 60);

            streamTimerB = setInterval(function() {
                if (idxB < textB.length) {
                    var chunkSize = Math.floor(Math.random() * 3) + 2;
                    idxB = Math.min(textB.length, idxB + chunkSize);
                    elB.innerHTML = escapeHtml(textB.substring(0, idxB)) + '<span class="cursor"></span>';
                    notifyBridgeProgress();
                } else {
                    doneB = true;
                    clearInterval(streamTimerB);
                    elB.innerHTML = escapeHtml(textB);
                    checkBothFinished(doneA, doneB, textA, textB);
                }
            }, 75);
        }

        function notifyBridgeProgress() {
            var chat = document.getElementById('chatContainer');
            chat.scrollTop = chat.scrollHeight;
            var totalChars = chat.innerText.length;
            if (window.ArenaBridge && window.ArenaBridge.onStreamProgress) {
                window.ArenaBridge.onStreamProgress(totalChars, true, "Model A 正在生成...", "Model B 正在生成...");
            }
        }

        function checkBothFinished(doneA, doneB, textA, textB) {
            if (doneA && doneB) {
                isStreaming = false;
                document.getElementById('sendBtn').disabled = false;
                document.getElementById('stopBtn').style.display = 'none';
                document.getElementById('status-indicator').innerText = '✅ 回复生成完毕';

                var chat = document.getElementById('chatContainer');
                var totalChars = chat.innerText.length;

                // Signal stopping to Android Bridge
                if (window.ArenaBridge && window.ArenaBridge.onGenerationStopped) {
                    window.ArenaBridge.onGenerationStopped(
                        totalChars,
                        textA.substring(0, 150),
                        textB.substring(0, 150),
                        "SANDBOX_DUAL_STREAM_COMPLETE"
                    );
                }

                // Capture full turn for conversation database
                if (window.ArenaBridge && window.ArenaBridge.onFullTurnCaptured) {
                    window.ArenaBridge.onFullTurnCaptured(
                        lastUserPrompt || "第 " + currentTurn + " 轮引导",
                        textA,
                        textB,
                        "Model A (Gemini 1.5 Pro)",
                        "Model B (Claude 3.5 Sonnet)"
                    );
                }
            }
        }

        function stopGeneration() {
            if (streamTimerA) clearInterval(streamTimerA);
            if (streamTimerB) clearInterval(streamTimerB);
            isStreaming = false;
            document.getElementById('sendBtn').disabled = false;
            document.getElementById('stopBtn').style.display = 'none';
            document.getElementById('status-indicator').innerText = '⏹️ 已手动停止';
            if (window.ArenaBridge && window.ArenaBridge.onGenerationStopped) {
                window.ArenaBridge.onGenerationStopped(0, "手动停止", "", "MANUAL_STOP");
            }
        }

        function escapeHtml(text) {
            return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
        }

        window.ArenaPilotBridge_injectPrompt = function(promptText) {
            var input = document.getElementById('promptInput');
            if (input) {
                input.value = promptText;
                setTimeout(function() {
                    handleUserSubmit();
                    if (window.ArenaBridge && window.ArenaBridge.onPromptInjected) {
                        window.ArenaBridge.onPromptInjected(promptText, true, "Sandbox prompt injected successfully");
                    }
                }, 400);
                return true;
            }
            return false;
        };

        window.ArenaPilotBridge_newBattle = function() {
            var chat = document.getElementById('chatContainer');
            chat.innerHTML = '<div class="turn-banner"><span class="turn-chip">新对战已开始</span></div>';
            document.getElementById('starterBar').style.display = 'flex';
            currentTurn = 0;
            return true;
        };

        window.onload = function() {
            if (window.ArenaBridge && window.ArenaBridge.onPageReady) {
                window.ArenaBridge.onPageReady("LMSYS Arena 对战沙盒", "sandbox://lmarena.ai");
            }
        };
    </script>
</body>
</html>
        """.trimIndent()
    }
}
