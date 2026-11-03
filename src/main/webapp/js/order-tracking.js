(() => {
    const root = document.querySelector('[data-tracking-code]');
    if (!root) return;
    const code = root.dataset.trackingCode;
    const contextPath = root.dataset.contextPath || '';
    const timeline = document.getElementById('trackingTimeline');
    const status = document.getElementById('trackingStatus');
    const payment = document.getElementById('trackingPayment');
    const shipping = document.getElementById('trackingShipping');
    const refreshStatus = document.getElementById('trackingRefreshStatus');
    let busy = false;
    const copyButton = document.querySelector('[data-copy-tracking]');
    copyButton?.addEventListener('click', async () => {
        const input = document.getElementById('trackingShareUrl');
        if (!input) return;
        try {
            await navigator.clipboard.writeText(input.value);
            copyButton.textContent = 'Đã sao chép';
        } catch (error) {
            input.focus(); input.select();
            try { if (document.execCommand('copy')) copyButton.textContent = 'Đã sao chép'; }
            catch (ignored) { }
        }
    });

    function appendText(parent, tag, value, className) {
        const child = document.createElement(tag);
        if (className) child.className = className;
        child.textContent = value == null ? '' : String(value);
        parent.appendChild(child);
        return child;
    }

    async function refresh() {
        if (busy || document.visibilityState === 'hidden') return;
        busy = true;
        try {
            const url = `${contextPath}/api/orders/tracking?code=${encodeURIComponent(code)}`;
            const response = await fetch(url, {headers: {'Accept': 'application/json'}, credentials: 'same-origin', cache: 'no-store'});
            if (!response.ok) throw new Error('Không thể tải cập nhật.');
            const result = await response.json();
            if (!result.success) throw new Error('Không tìm thấy đơn hàng.');
            status.textContent = result.status || '';
            payment.textContent = result.paymentStatus || '';
            shipping.textContent = result.shippingStatus || '';
            if (timeline && Array.isArray(result.timeline)) {
                timeline.replaceChildren();
                result.timeline.forEach(event => {
                    const li = document.createElement('li');
                    const content = document.createElement('div');
                    appendText(content, 'strong', event.title);
                    if (event.details) appendText(content, 'p', event.details);
                    appendText(content, 'small', `${event.source || ''} · ${event.when || ''}`);
                    li.appendChild(content);
                    timeline.appendChild(li);
                });
            }
            refreshStatus.textContent = `Đã kiểm tra cập nhật lúc ${new Date().toLocaleTimeString('vi-VN')}.`;
        } catch (error) {
            refreshStatus.textContent = 'Không tải được cập nhật mới. Trang sẽ thử lại sau.';
        } finally {
            busy = false;
        }
    }

    window.setInterval(refresh, 30000);
})();
