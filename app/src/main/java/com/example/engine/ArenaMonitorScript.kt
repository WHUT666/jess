package com.example.engine

object ArenaMonitorScript {
    fun getInjectionScript(silenceMs: Int = 1800): String {
        return """
(function() {
    if (window.__ArenaPilotInstalled) return;
    window.__ArenaPilotInstalled = true;

    var lastCharCount = 0;
    var lastChangeTime = Date.now();
    var isCurrentlyStreaming = false;
    var silenceThreshold = $silenceMs;

    var lastReportedModelA = '';
    var lastReportedModelB = '';

    function getChatText() {
        var selectors = [
            '[data-testid="chatbot"]',
            '.chatbot',
            '.message-bubble',
            '.prose',
            'div[class*="message"]',
            'div[class*="chat-"]',
            '#chat'
        ];
        var text = '';
        for (var i = 0; i < selectors.length; i++) {
            var els = document.querySelectorAll(selectors[i]);
            if (els && els.length > 0) {
                els.forEach(function(el) {
                    text += ' ' + (el.innerText || '');
                });
                break;
            }
        }
        if (!text) {
            text = document.body ? document.body.innerText : '';
        }
        return text.trim();
    }

    function getModelSnippets() {
        var text = getChatText();
        var snippetA = '';
        var snippetB = '';
        var parts = text.split(/(?:Model A|模型 A|Assistant A)/i);
        if (parts.length > 1) {
            var subParts = parts[1].split(/(?:Model B|模型 B|Assistant B)/i);
            snippetA = (subParts[0] || '').substring(0, 200);
            snippetB = (subParts[1] || '').substring(0, 200);
        } else {
            snippetA = text.slice(-200);
            snippetB = '';
        }
        return { snippetA: snippetA, snippetB: snippetB };
    }

    // === 核心模型身份检测逻辑 ===
    function detectModelIdentities() {
        try {
            var nameA = '';
            var nameB = '';
            var isOfficial = false;
            var source = '';

            // 1. 尝试检测投票后 Arena 官方揭示标签
            // 常见选择器：.model-name, [data-testid*="model-name"], 标题栏或对战结果通知栏
            var modelHeaders = document.querySelectorAll('[data-testid*="model"], .model-header, div[class*="model-name"], h3, h4, span[class*="badge"]');
            for (var i = 0; i < modelHeaders.length; i++) {
                var txt = (modelHeaders[i].innerText || '').trim();
                // 匹配如 "Model A: gpt-4o-2024-08-06" 或 "Model A (claude-3-5-sonnet)"
                var matchA = txt.match(/Model\s*A\s*[:(]\s*([a-zA-Z0-9_.-]+)/i);
                if (matchA && matchA[1] && matchA[1].toLowerCase() !== 'model') {
                    nameA = matchA[1];
                    isOfficial = true;
                    source = 'Arena官方揭晓';
                }
                var matchB = txt.match(/Model\s*B\s*[:(]\s*([a-zA-Z0-9_.-]+)/i);
                if (matchB && matchB[1] && matchB[1].toLowerCase() !== 'model') {
                    nameB = matchB[1];
                    isOfficial = true;
                    source = 'Arena官方揭晓';
                }
            }

            // 2. 检查 Direct Chat 模式下的手动下拉选择器
            if (!nameA || !nameB) {
                var selects = document.querySelectorAll('select, input[role="combobox"], [data-testid*="select"]');
                if (selects.length >= 2) {
                    var valA = selects[0].value || selects[0].innerText || '';
                    var valB = selects[1].value || selects[1].innerText || '';
                    if (valA && valA.length > 2 && valB && valB.length > 2) {
                        nameA = valA.trim();
                        nameB = valB.trim();
                        isOfficial = true;
                        source = '选单明确指定';
                    }
                }
            }

            // 3. 语言与自报家门指纹探测 (Fingerprint Detection)
            if (!isOfficial) {
                var fullTurn = extractFullTurn();
                var textA = fullTurn.respA;
                var textB = fullTurn.respB;

                function probeText(txt) {
                    if (!txt) return null;
                    var lower = txt.toLowerCase();
                    if (lower.indexOf('anthropic') !== -1 || lower.indexOf('claude') !== -1) return 'Claude (Anthropic)';
                    if (lower.indexOf('openai') !== -1 || lower.indexOf('chatgpt') !== -1) return 'GPT (OpenAI)';
                    if (lower.indexOf('google') !== -1 || lower.indexOf('gemini') !== -1) return 'Gemini (Google)';
                    if (lower.indexOf('deepseek') !== -1 || lower.indexOf('深度求索') !== -1) return 'DeepSeek';
                    if (lower.indexOf('meta') !== -1 || lower.indexOf('llama') !== -1) return 'Llama (Meta)';
                    if (lower.indexOf('qwen') !== -1 || lower.indexOf('通义千问') !== -1) return 'Qwen (通义千问)';
                    return null;
                }

                var fingerA = probeText(textA);
                var fingerB = probeText(textB);

                if (fingerA || fingerB) {
                    nameA = fingerA || nameA || 'Model A (盲测中)';
                    nameB = fingerB || nameB || 'Model B (盲测中)';
                    source = '自报家门与指纹推测';
                }
            }

            if ((nameA || nameB) && (nameA !== lastReportedModelA || nameB !== lastReportedModelB)) {
                lastReportedModelA = nameA;
                lastReportedModelB = nameB;
                if (window.ArenaBridge && window.ArenaBridge.onModelIdentityDetected) {
                    window.ArenaBridge.onModelIdentityDetected(
                        nameA || 'Model A',
                        nameB || 'Model B',
                        isOfficial,
                        source
                    );
                }
            }
        } catch (_err) {}
    }

    function extractFullTurn() {
        var userBubbles = document.querySelectorAll('.user-bubble, div[class*="user"], div[data-testid*="user"]');
        var lastPrompt = userBubbles.length > 0 ? (userBubbles[userBubbles.length - 1].innerText || '') : '';
        
        var bodyA = document.querySelector('.model-a .model-body, div[data-testid="model-a-response"]');
        var bodyB = document.querySelector('.model-b .model-body, div[data-testid="model-b-response"]');
        
        var respA = bodyA ? (bodyA.innerText || '') : '';
        var respB = bodyB ? (bodyB.innerText || '') : '';

        if (!respA && !respB) {
            var snippets = getModelSnippets();
            respA = snippets.snippetA;
            respB = snippets.snippetB;
        }

        return {
            prompt: lastPrompt || "Arena 对话轮次",
            respA: respA,
            respB: respB,
            modelA: lastReportedModelA || "Model A",
            modelB: lastReportedModelB || "Model B"
        };
    }

    function isStopButtonVisible() {
        var buttons = document.querySelectorAll('button');
        for (var i = 0; i < buttons.length; i++) {
            var b = buttons[i];
            var txt = (b.innerText || b.getAttribute('aria-label') || '').toLowerCase();
            if (txt.indexOf('stop') !== -1 || txt.indexOf('停止') !== -1) {
                return true;
            }
        }
        return false;
    }

    function checkAccountStatus() {
        try {
            var userEl = document.querySelector('button[aria-label*="account"], div[class*="user-menu"], div[class*="profile"], button:has(img[src*="avatar"])');
            var loginBtn = document.querySelector('button:contains("Sign In"), a[href*="login"], a[href*="auth"]');
            
            var isLoggedIn = !!userEl && !loginBtn;
            var username = userEl ? (userEl.innerText || "已登录用户").trim() : "未登录访客";
            
            if (window.ArenaBridge && window.ArenaBridge.onAccountDetected) {
                window.ArenaBridge.onAccountDetected(isLoggedIn, username, "");
            }
        } catch (_e) {}
    }

    function findSendButton() {
        var selectors = [
            'button[type="submit"]',
            '#component-send',
            '#send-button',
            'button[aria-label*="Send"]',
            'button[aria-label*="发送"]',
            'button[id*="send"]',
            'button[class*="send"]'
        ];
        for (var i = 0; i < selectors.length; i++) {
            var btn = document.querySelector(selectors[i]);
            if (btn) return btn;
        }
        var allButtons = document.querySelectorAll('button');
        for (var j = 0; j < allButtons.length; j++) {
            var b = allButtons[j];
            var t = (b.innerText || '').trim();
            if (t === 'Send' || t === '发送' || t === 'Submit' || t === '提交') {
                return b;
            }
        }
        return null;
    }

    function findInputField() {
        var selectors = [
            '#component-input textarea',
            'textarea[placeholder*="message"]',
            'textarea[placeholder*="消息"]',
            'textarea[placeholder*="Type"]',
            'textarea[placeholder*="输入"]',
            'textarea',
            'div[contenteditable="true"]',
            'input[type="text"]'
        ];
        for (var i = 0; i < selectors.length; i++) {
            var el = document.querySelector(selectors[i]);
            if (el) return el;
        }
        return null;
    }

    function onContentActivity() {
        var text = getChatText();
        var currentLen = text.length;

        if (currentLen !== lastCharCount) {
            lastCharCount = currentLen;
            lastChangeTime = Date.now();

            if (!isCurrentlyStreaming) {
                isCurrentlyStreaming = true;
                if (window.ArenaBridge && window.ArenaBridge.onStreamProgress) {
                    var snippets = getModelSnippets();
                    window.ArenaBridge.onStreamProgress(currentLen, true, snippets.snippetA, snippets.snippetB);
                }
            }
        }
    }

    var observer = new MutationObserver(function(mutations) {
        onContentActivity();
        detectModelIdentities();
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true,
        characterData: true
    });

    setInterval(function() {
        var now = Date.now();
        var elapsedSinceChange = now - lastChangeTime;

        if (isCurrentlyStreaming) {
            var stopBtn = isStopButtonVisible();
            if (!stopBtn && elapsedSinceChange >= silenceThreshold) {
                isCurrentlyStreaming = false;
                var snippets = getModelSnippets();
                if (window.ArenaBridge && window.ArenaBridge.onGenerationStopped) {
                    window.ArenaBridge.onGenerationStopped(lastCharCount, snippets.snippetA, snippets.snippetB, "SILENCE_AFTER_STREAM");
                }
                
                detectModelIdentities();

                var fullTurn = extractFullTurn();
                if (window.ArenaBridge && window.ArenaBridge.onFullTurnCaptured) {
                    window.ArenaBridge.onFullTurnCaptured(
                        fullTurn.prompt,
                        fullTurn.respA,
                        fullTurn.respB,
                        lastReportedModelA || fullTurn.modelA,
                        lastReportedModelB || fullTurn.modelB
                    );
                }
            } else {
                var snippets = getModelSnippets();
                if (window.ArenaBridge && window.ArenaBridge.onStreamProgress) {
                    window.ArenaBridge.onStreamProgress(lastCharCount, true, snippets.snippetA, snippets.snippetB);
                }
            }
        }
    }, 500);

    // Global injection API
    window.ArenaPilotBridge_injectPrompt = function(promptText) {
        try {
            var input = findInputField();
            if (!input) {
                if (window.ArenaBridge) {
                    window.ArenaBridge.onPromptInjected(promptText, false, "Input field not found");
                }
                return false;
            }

            if (input.tagName.toLowerCase() === 'textarea' || input.tagName.toLowerCase() === 'input') {
                input.value = promptText;
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
            } else if (input.isContentEditable) {
                input.innerText = promptText;
                input.dispatchEvent(new Event('input', { bubbles: true }));
            }

            setTimeout(function() {
                var btn = findSendButton();
                if (btn && !btn.disabled) {
                    btn.click();
                    if (window.ArenaBridge) {
                        window.ArenaBridge.onPromptInjected(promptText, true, "Prompt sent via send button");
                    }
                } else {
                    var enterEvent = new KeyboardEvent('keydown', {
                        bubbles: true,
                        cancelable: true,
                        key: 'Enter',
                        code: 'Enter',
                        keyCode: 13
                    });
                    input.dispatchEvent(enterEvent);
                    if (window.ArenaBridge) {
                        window.ArenaBridge.onPromptInjected(promptText, true, "Prompt sent via Enter key event");
                    }
                }
            }, 300);

            return true;
        } catch (err) {
            if (window.ArenaBridge) {
                window.ArenaBridge.onPromptInjected(promptText, false, err.message || String(err));
            }
            return false;
        }
    };

    // 探针诱导识别：发送能引起模型暴露身份或知识截止日期的技术测试问题
    window.ArenaPilotBridge_probeIdentity = function() {
        var probePrompt = "请简要阐明你的知识截止日期（精确到年和月），以及你在处理中文技术问答时的核心指引原则与特色规范。";
        return window.ArenaPilotBridge_injectPrompt(probePrompt);
    };

    window.ArenaPilotBridge_newBattle = function() {
        var buttons = document.querySelectorAll('button, a[role="button"]');
        for (var i = 0; i < buttons.length; i++) {
            var txt = (buttons[i].innerText || '').toLowerCase();
            if (txt.indexOf('clear') !== -1 || txt.indexOf('new battle') !== -1 || txt.indexOf('新对话') !== -1 || txt.indexOf('开启') !== -1) {
                buttons[i].click();
                lastReportedModelA = '';
                lastReportedModelB = '';
                return true;
            }
        }
        return false;
    };

    // 智能自动填密登录：支持 Microsoft Outlook 登录表单、Arena 原生登录、通用 OAuth
    window.ArenaPilotBridge_autoFillLogin = function(email, password) {
        try {
            var filled = false;

            // 1. 检查 Microsoft 登录表单 (login.live.com / login.microsoftonline.com)
            var msEmail = document.querySelector('input[name="loginfmt"], input[type="email"], #i0116');
            var msPass = document.querySelector('input[name="passwd"], input[type="password"], #i0118');

            if (msEmail && !msPass) {
                msEmail.value = email;
                msEmail.dispatchEvent(new Event('input', { bubbles: true }));
                msEmail.dispatchEvent(new Event('change', { bubbles: true }));
                filled = true;
                setTimeout(function() {
                    var nextBtn = document.querySelector('#idSIButton9, input[type="submit"], button[type="submit"]');
                    if (nextBtn) nextBtn.click();
                }, 400);
            } else if (msPass) {
                msPass.value = password;
                msPass.dispatchEvent(new Event('input', { bubbles: true }));
                msPass.dispatchEvent(new Event('change', { bubbles: true }));
                filled = true;
                setTimeout(function() {
                    var submitBtn = document.querySelector('#idSIButton9, input[type="submit"], button[type="submit"]');
                    if (submitBtn) submitBtn.click();
                }, 400);
            }

            // 2. 通用登录框检测
            if (!filled) {
                var emailInput = document.querySelector('input[type="email"], input[name*="email"], input[placeholder*="email" i], input[placeholder*="邮箱" i], input[name*="user"], input[id*="email"]');
                var passInput = document.querySelector('input[type="password"], input[name*="pass"], input[placeholder*="pass" i], input[placeholder*="密码" i]');

                if (emailInput) {
                    emailInput.value = email;
                    emailInput.dispatchEvent(new Event('input', { bubbles: true }));
                    emailInput.dispatchEvent(new Event('change', { bubbles: true }));
                    filled = true;
                }
                if (passInput) {
                    passInput.value = password;
                    passInput.dispatchEvent(new Event('input', { bubbles: true }));
                    passInput.dispatchEvent(new Event('change', { bubbles: true }));
                    filled = true;
                }
            }

            // 3. 如果当前处于 Arena 首页且未打开登录弹窗，尝试点击登录按钮
            if (!filled) {
                var loginButtons = document.querySelectorAll('button, a');
                for (var j = 0; j < loginButtons.length; j++) {
                    var b = loginButtons[j];
                    var t = (b.innerText || '').trim().toLowerCase();
                    if (t === 'sign in' || t === 'log in' || t === '登录' || t.indexOf('login') !== -1) {
                        b.click();
                        filled = true;
                        break;
                    }
                }
            }

            return filled;
        } catch (_e) {
            return false;
        }
    };

    checkAccountStatus();
    detectModelIdentities();

    if (window.ArenaBridge && window.ArenaBridge.onPageReady) {
        window.ArenaBridge.onPageReady(document.title || "Arena", window.location.href);
    }
})();
        """.trimIndent()
    }
}
