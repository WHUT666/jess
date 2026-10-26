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
            modelA: "Model A",
            modelB: "Model B"
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
            // Check for user email / profile indicators in Arena navigation
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
                // Capture full turn for conversation management
                var fullTurn = extractFullTurn();
                if (window.ArenaBridge && window.ArenaBridge.onFullTurnCaptured) {
                    window.ArenaBridge.onFullTurnCaptured(
                        fullTurn.prompt,
                        fullTurn.respA,
                        fullTurn.respB,
                        fullTurn.modelA,
                        fullTurn.modelB
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

    window.ArenaPilotBridge_newBattle = function() {
        var buttons = document.querySelectorAll('button');
        for (var i = 0; i < buttons.length; i++) {
            var txt = (buttons[i].innerText || '').toLowerCase();
            if (txt.indexOf('clear') !== -1 || txt.indexOf('new battle') !== -1 || txt.indexOf('新对话') !== -1) {
                buttons[i].click();
                return true;
            }
        }
        return false;
    };

    checkAccountStatus();

    if (window.ArenaBridge && window.ArenaBridge.onPageReady) {
        window.ArenaBridge.onPageReady(document.title || "Arena", window.location.href);
    }
})();
        """.trimIndent()
    }
}
