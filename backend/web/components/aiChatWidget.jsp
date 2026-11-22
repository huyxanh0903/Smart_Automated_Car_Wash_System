<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!-- ========================================== -->
<!-- SMART AI CAR WASH CONCIERGE CHAT WIDGET    -->
<!-- ========================================== -->

<style>
    #ai-chat-btn {
        position: fixed;
        bottom: 24px;
        right: 24px;
        z-index: 9999;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 20px;
        background: linear-gradient(135deg, #2563eb, #06b6d4);
        color: #ffffff;
        border-radius: 9999px;
        box-shadow: 0 10px 25px -5px rgba(37, 99, 235, 0.4), 0 8px 10px -6px rgba(37, 99, 235, 0.2);
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        border: 2px solid rgba(255, 255, 255, 0.2);
        font-family: inherit;
        font-size: 14px;
        font-weight: 600;
    }
    #ai-chat-btn:hover {
        transform: translateY(-3px) scale(1.02);
        box-shadow: 0 15px 30px -5px rgba(37, 99, 235, 0.5);
    }
    #ai-chat-btn .pulse-dot {
        width: 10px;
        height: 10px;
        background-color: #22c55e;
        border-radius: 50%;
        border: 2px solid #ffffff;
        animation: ai-pulse 2s infinite;
    }
    @keyframes ai-pulse {
        0%, 100% { transform: scale(1); opacity: 1; }
        50% { transform: scale(1.3); opacity: 0.8; }
    }

    #ai-chat-window {
        position: fixed;
        bottom: 88px;
        right: 24px;
        width: 380px;
        max-width: calc(100vw - 32px);
        height: 540px;
        max-height: calc(100vh - 120px);
        background: #ffffff;
        border-radius: 20px;
        box-shadow: 0 20px 40px -10px rgba(15, 23, 42, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.05);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        z-index: 9999;
        opacity: 0;
        transform: scale(0.9) translateY(20px);
        pointer-events: none;
        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
        font-family: inherit;
    }
    #ai-chat-window.open {
        opacity: 1;
        transform: scale(1) translateY(0);
        pointer-events: auto;
    }

    .ai-chat-header {
        background: linear-gradient(135deg, #1e3a8a, #2563eb);
        color: #ffffff;
        padding: 16px 18px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }
    .ai-chat-header .bot-info {
        display: flex;
        align-items: center;
        gap: 12px;
    }
    .ai-chat-header .bot-avatar {
        width: 38px;
        height: 38px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
    }
    .ai-chat-header .bot-title {
        font-size: 15px;
        font-weight: 700;
        line-height: 1.2;
    }
    .ai-chat-header .bot-subtitle {
        font-size: 11px;
        color: #93c5fd;
        display: flex;
        align-items: center;
        gap: 4px;
    }
    .ai-chat-header .close-btn {
        background: transparent;
        border: none;
        color: #ffffff;
        cursor: pointer;
        padding: 6px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;
    }
    .ai-chat-header .close-btn:hover {
        background: rgba(255, 255, 255, 0.2);
    }

    .ai-chat-body {
        flex: 1;
        padding: 16px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 12px;
        background: #f8fafc;
    }

    .ai-msg {
        max-width: 85%;
        padding: 10px 14px;
        border-radius: 14px;
        font-size: 13.5px;
        line-height: 1.5;
        word-break: break-word;
        white-space: pre-line;
    }
    .ai-msg.bot {
        align-self: flex-start;
        background: #ffffff;
        color: #1e293b;
        border-bottom-left-radius: 4px;
        border: 1px solid #e2e8f0;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    }
    .ai-msg.user {
        align-self: flex-end;
        background: #2563eb;
        color: #ffffff;
        border-bottom-right-radius: 4px;
    }

    .ai-chips-container {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-top: 4px;
    }
    .ai-chip {
        font-size: 11.5px;
        padding: 6px 10px;
        background: #e0e7ff;
        color: #1d4ed8;
        border: 1px solid #c7d2fe;
        border-radius: 9999px;
        cursor: pointer;
        transition: all 0.15s;
        text-align: left;
    }
    .ai-chip:hover {
        background: #dbeafe;
        color: #1e40af;
        border-color: #93c5fd;
    }

    .ai-typing-indicator {
        align-self: flex-start;
        display: none;
        align-items: center;
        gap: 4px;
        background: #ffffff;
        padding: 10px 14px;
        border-radius: 14px;
        border: 1px solid #e2e8f0;
    }
    .ai-typing-indicator .dot {
        width: 6px;
        height: 6px;
        background: #94a3b8;
        border-radius: 50%;
        animation: bounce 1.4s infinite ease-in-out both;
    }
    .ai-typing-indicator .dot:nth-child(1) { animation-delay: -0.32s; }
    .ai-typing-indicator .dot:nth-child(2) { animation-delay: -0.16s; }
    @keyframes bounce {
        0%, 80%, 100% { transform: scale(0); }
        40% { transform: scale(1); }
    }

    .ai-chat-footer {
        padding: 12px;
        background: #ffffff;
        border-top: 1px solid #e2e8f0;
        display: flex;
        gap: 8px;
    }
    .ai-chat-input {
        flex: 1;
        padding: 10px 14px;
        font-size: 13.5px;
        border: 1px solid #cbd5e1;
        border-radius: 9999px;
        outline: none;
        transition: border-color 0.2s;
    }
    .ai-chat-input:focus {
        border-color: #2563eb;
    }
    .ai-chat-send {
        background: #2563eb;
        color: #ffffff;
        border: none;
        border-radius: 50%;
        width: 38px;
        height: 38px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background 0.2s;
    }
    .ai-chat-send:hover {
        background: #1d4ed8;
    }
</style>

<!-- Floating AI Button -->
<button id="ai-chat-btn" onclick="toggleAiChat()" aria-label="Mở Trợ lý ảo AI Car Wash">
    <span class="pulse-dot"></span>
    <span>Trợ lý AI</span>
    <span>✨</span>
</button>

<!-- AI Chat Box Window -->
<div id="ai-chat-window">
    <div class="ai-chat-header">
        <div class="bot-info">
            <div class="bot-avatar">🤖</div>
            <div>
                <div class="bot-title">AutoWash AI Concierge</div>
                <div class="bot-subtitle">
                    <span style="display:inline-block; width:6px; height:6px; background:#22c55e; border-radius:50%;"></span>
                    <span>Tư vấn chăm sóc & đặt lịch 24/7</span>
                </div>
            </div>
        </div>
        <button class="close-btn" onclick="toggleAiChat()" title="Đóng">✕</button>
    </div>

    <div class="ai-chat-body" id="ai-messages-container">
        <!-- Lời chào đầu tiên -->
        <div class="ai-msg bot">
            👋 Chào bạn! Tôi là <strong>Smart Wash Bot</strong> - Trợ lý chăm sóc ô tô thông minh.<br><br>
            Tôi có thể tư vấn gói rửa xe phù hợp với xe của bạn, giải thích chính sách hủy/đổi lịch và ưu đãi thành viên.<br><br>
            <em>Bạn muốn hỏi về điều gì?</em>
            <div class="ai-chips-container">
                <button type="button" class="ai-chip" onclick="sendQuickPrompt(this)">🚗 Xe tôi nên chọn gói rửa nào?</button>
                <button type="button" class="ai-chip" onclick="sendQuickPrompt(this)">⭐ Quyền lợi hạng thành viên của tôi?</button>
                <button type="button" class="ai-chip" onclick="sendQuickPrompt(this)">🌧️ Kính xe bị ố mốc xử lý sao?</button>
                <button type="button" class="ai-chip" onclick="sendQuickPrompt(this)">❌ Quy định đổi và hủy lịch?</button>
            </div>
        </div>

        <div class="ai-typing-indicator" id="ai-typing">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
        </div>
    </div>

    <form class="ai-chat-footer" onsubmit="handleAiSubmit(event)">
        <input type="text" id="ai-input" class="ai-chat-input" placeholder="Nhập câu hỏi chăm sóc xe..." autocomplete="off" />
        <button type="submit" class="ai-chat-send" title="Gửi câu hỏi">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
        </button>
    </form>
</div>

<script>
    const aiContextPath = '${pageContext.request.contextPath}';

    function toggleAiChat() {
        const win = document.getElementById('ai-chat-window');
        win.classList.toggle('open');
        if (win.classList.contains('open')) {
            document.getElementById('ai-input').focus();
        }
    }

    function sendQuickPrompt(btn) {
        const text = btn.innerText.replace(/^[^\w\s\u00C0-\u1EF9]+/, '').trim();
        document.getElementById('ai-input').value = text;
        handleAiSubmit();
    }

    function appendMessage(sender, text) {
        const container = document.getElementById('ai-messages-container');
        const typing = document.getElementById('ai-typing');
        const msgDiv = document.createElement('div');
        msgDiv.className = 'ai-msg ' + sender;
        msgDiv.innerHTML = text.replace(/\n/g, '<br/>');
        container.insertBefore(msgDiv, typing);
        container.scrollTop = container.scrollHeight;
    }

    function handleAiSubmit(e) {
        if (e) e.preventDefault();
        const input = document.getElementById('ai-input');
        const message = input.value.trim();
        if (!message) return;

        appendMessage('user', message);
        input.value = '';

        const typing = document.getElementById('ai-typing');
        typing.style.display = 'flex';
        const container = document.getElementById('ai-messages-container');
        container.scrollTop = container.scrollHeight;

        fetch(aiContextPath + '/ai-chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            body: 'message=' + encodeURIComponent(message)
        })
        .then(res => res.json())
        .then(data => {
            typing.style.display = 'none';
            if (data && data.reply) {
                appendMessage('bot', data.reply);
            } else {
                appendMessage('bot', 'Xin lỗi, hiện tại tôi chưa xử lý được câu hỏi này.');
            }
        })
        .catch(err => {
            typing.style.display = 'none';
            console.error('AI Chat Error:', err);
            appendMessage('bot', '⚠️ Không thể kết nối tới máy chủ AI. Vui lòng kiểm tra lại mạng hoặc thử lại sau.');
        });
    }
</script>
