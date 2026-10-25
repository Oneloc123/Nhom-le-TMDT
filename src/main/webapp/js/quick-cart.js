(function () {
    'use strict';

    const forms = document.querySelectorAll('.js-quick-cart-form');
    if (!forms.length) return;

    const toast = document.createElement('div');
    toast.className = 'quick-cart-toast';
    toast.setAttribute('role', 'status');
    toast.setAttribute('aria-live', 'polite');
    document.body.appendChild(toast);
    let toastTimer;

    function showToast(message, error) {
        window.clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = 'quick-cart-toast is-visible ' + (error ? 'is-error' : 'is-success');
        toastTimer = window.setTimeout(function () {
            toast.classList.remove('is-visible');
        }, 3200);
    }

    function updateCartBadge(quantity) {
        const badge = document.getElementById('cartBadge');
        if (!badge) return;
        badge.textContent = String(quantity);
        badge.classList.remove('badge-bump');
        void badge.offsetWidth;
        badge.classList.add('badge-bump');
    }

    forms.forEach(function (form) {
        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            const button = form.querySelector('button[type="submit"]');
            if (!button || button.disabled) return;

            const originalText = button.textContent;
            button.disabled = true;
            button.classList.add('is-loading');
            button.textContent = '…';

            try {
                const payload = new URLSearchParams(new FormData(form));
                payload.set('ajax', '1');
                const endpoint = new URL(form.getAttribute('action'), window.location.href).href;
                const response = await fetch(endpoint, {
                    method: 'POST',
                    body: payload,
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });
                const contentType = response.headers.get('content-type') || '';
                if (!contentType.includes('application/json')) {
                    throw new Error('Phản hồi từ máy chủ không hợp lệ. Vui lòng thử lại.');
                }
                const data = await response.json();
                if (!response.ok || !data.success) {
                    throw new Error(data.message || 'Không thể thêm sản phẩm vào giỏ hàng.');
                }
                updateCartBadge(data.cartTotalQuantity);
                showToast(data.message || 'Đã thêm sản phẩm vào giỏ hàng.', false);
                button.textContent = '✓';
            } catch (error) {
                showToast(error.message || 'Không thể thêm sản phẩm vào giỏ hàng.', true);
                button.textContent = '!';
            } finally {
                window.setTimeout(function () {
                    button.disabled = false;
                    button.classList.remove('is-loading');
                    button.textContent = originalText;
                }, 650);
            }
        });
    });
})();
