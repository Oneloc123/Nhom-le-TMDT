(function () {
    'use strict';

    const widget = document.getElementById('pkChatWidget');
    const launcher = document.getElementById('pkChatLauncher');
    const panel = document.getElementById('pkChatPanel');
    const closeButton = document.getElementById('pkChatClose');
    const newChatButton = document.getElementById('pkChatNew');
    const statusNode = document.getElementById('pkChatStatus');
    const messages = document.getElementById('pkChatMessages');
    const form = document.getElementById('pkChatForm');
    const input = document.getElementById('pkChatInput');
    const sendButton = document.getElementById('pkChatSend');
    const quickBar = document.getElementById('pkChatQuick');
    if (!widget || !launcher || !panel || !messages || !form || !input) return;

    const base = widget.dataset.contextPath || '';
    const apiPath = base + (widget.dataset.apiPath || '/api/chatbot/message');
    const csrfToken = widget.dataset.csrfToken || '';
    let busy = false;
    let conversationId = null;

    function createId() {
        if (window.crypto && typeof window.crypto.randomUUID === 'function') {
            return window.crypto.randomUUID();
        }
        return 'chat_' + Date.now() + '_' + Math.random().toString(36).slice(2, 12);
    }

    function scrollToBottom() {
        messages.scrollTop = messages.scrollHeight;
    }

    function addMessage(role, text, actions, options) {
        const settings = options || {};
        const line = document.createElement('div');
        line.className = 'pk-chat-line' + (role === 'user' ? ' is-user' : '');
        if (settings.error) line.classList.add('is-error');

        const bubble = document.createElement('div');
        bubble.className = 'pk-chat-bubble';
        bubble.textContent = String(text || '');
        if (role === 'typing') {
            bubble.classList.add('pk-chat-typing');
            bubble.setAttribute('aria-label', 'Đang soạn câu trả lời');
            bubble.replaceChildren();
            for (let i = 0; i < 3; i++) {
                const dot = document.createElement('span');
                dot.className = 'pk-chat-typing-dot';
                dot.setAttribute('aria-hidden', 'true');
                bubble.append(dot);
            }
        }
        line.append(bubble);

        if (Array.isArray(actions) && actions.length) {
            const actionBox = document.createElement('div');
            actionBox.className = 'pk-chat-actions';
            actions.forEach(function (action) {
                if (!action || typeof action.label !== 'string' || typeof action.path !== 'string') return;
                if (!action.path.startsWith('/') || action.path.startsWith('//')) return;
                const link = document.createElement('a');
                link.className = 'pk-chat-action';
                link.textContent = action.label;
                link.href = base + action.path;
                actionBox.append(link);
            });
            if (actionBox.childElementCount) line.append(actionBox);
        }

        if (settings.retry) {
            const retryBox = document.createElement('div');
            retryBox.className = 'pk-chat-actions';
            const retryButton = document.createElement('button');
            retryButton.type = 'button';
            retryButton.className = 'pk-chat-action pk-chat-retry';
            retryButton.textContent = 'Thử lại';
            retryButton.addEventListener('click', function () {
                if (!busy) sendQuestion(settings.retry.question, settings.retry.requestId);
            });
            retryBox.append(retryButton);
            line.append(retryBox);
        }

        messages.append(line);
        scrollToBottom();
        return line;
    }

    function setBusy(value, statusText) {
        busy = value;
        if (sendButton) sendButton.disabled = value;
        if (input) input.disabled = value;
        if (newChatButton) newChatButton.disabled = value;
        if (quickBar) {
            quickBar.querySelectorAll('button').forEach(function (button) {
                button.disabled = value;
            });
        }
        if (statusNode) {
            statusNode.textContent = statusText || 'Hỏi đáp nhanh về mua hàng và đơn in';
        }
        widget.classList.toggle('is-busy', value);
    }

    async function post(payload) {
        const body = new URLSearchParams();
        Object.keys(payload).forEach(function (key) {
            if (payload[key] != null) body.set(key, String(payload[key]));
        });
        body.set('csrfToken', csrfToken);

        const response = await fetch(apiPath, {
            method: 'POST',
            credentials: 'same-origin',
            cache: 'no-store',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: body.toString()
        });

        let data;
        try {
            data = await response.json();
        } catch (error) {
            throw new Error('Máy chủ trả về dữ liệu không hợp lệ. Vui lòng thử lại.');
        }

        if (!response.ok || !data || data.success !== true) {
            const message = data && data.error && data.error.message
                ? data.error.message
                : (data && data.message ? data.message : 'Không thể kết nối với trợ lý. Vui lòng thử lại.');
            const error = new Error(message);
            error.retryable = !data || !data.error || data.error.retryable !== false;
            throw error;
        }
        return data;
    }

    async function sendQuestion(question, requestId) {
        if (busy) return;
        const value = String(question || '').trim();
        if (!value) return;

        setBusy(true, 'Đang suy nghĩ…');
        const typing = addMessage('typing', '');
        try {
            const data = await post({
                action: 'message',
                question: value,
                requestId: requestId || createId()
            });
            typing.remove();
            conversationId = data.conversationId || conversationId;
            const message = data.message || {};
            addMessage('bot', message.text || 'Mình chưa có câu trả lời phù hợp.', message.actions || []);
            setBusy(false, message.fallback ? 'Mình có thể hướng dẫn thêm' : 'Trợ lý đã sẵn sàng');
        } catch (error) {
            typing.remove();
            addMessage('bot', error.message || 'Có lỗi xảy ra. Vui lòng thử lại.', [], {
                error: true,
                retry: error.retryable === false ? null : { question: value, requestId: requestId || createId() }
            });
            setBusy(false, 'Chưa gửi được câu trả lời');
        }
    }

    function ask(question) {
        const value = String(question || '').trim();
        if (!value || busy) return;
        addMessage('user', value);
        sendQuestion(value, createId());
    }

    function resetMessages(greeting, actions) {
        messages.replaceChildren();
        addMessage('bot', greeting, actions || []);
        if (quickBar) messages.append(quickBar);
        scrollToBottom();
    }

    async function startNewConversation() {
        if (busy) return;
        setBusy(true, 'Đang tạo cuộc trò chuyện…');
        try {
            const data = await post({ action: 'new', requestId: createId() });
            conversationId = data.conversationId || null;
            const message = data.message || {};
            resetMessages(message.text || 'Xin chào! Bạn cần hỗ trợ điều gì?', message.actions || []);
            setBusy(false, 'Cuộc trò chuyện mới');
        } catch (error) {
            addMessage('bot', error.message || 'Không thể bắt đầu cuộc trò chuyện mới.', [], { error: true });
            setBusy(false, 'Chưa thể tạo cuộc trò chuyện mới');
        }
    }

    function openPanel() {
        panel.hidden = false;
        launcher.setAttribute('aria-expanded', 'true');
        input.focus();
    }

    function closePanel() {
        panel.hidden = true;
        launcher.setAttribute('aria-expanded', 'false');
        launcher.focus();
    }

    launcher.addEventListener('click', function () {
        if (panel.hidden) openPanel();
        else closePanel();
    });
    if (closeButton) closeButton.addEventListener('click', closePanel);
    if (newChatButton) newChatButton.addEventListener('click', startNewConversation);

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        const question = input.value;
        if (!question.trim() || busy) return;
        input.value = '';
        ask(question);
    });

    if (quickBar) {
        quickBar.querySelectorAll('[data-chat-question]').forEach(function (button) {
            button.addEventListener('click', function () {
                ask(button.dataset.chatQuestion);
            });
        });
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !panel.hidden) closePanel();
    });
})();
