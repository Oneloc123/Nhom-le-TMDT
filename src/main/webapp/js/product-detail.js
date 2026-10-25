(function () {
    'use strict';

    const toast = document.getElementById('productToast');
    let toastTimer;

    function showToast(message, type) {
        if (!toast) return;
        window.clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = 'product-toast is-visible ' + (type === 'error' ? 'is-error' : 'is-success');
        toastTimer = window.setTimeout(function () {
            toast.classList.remove('is-visible');
        }, 3200);
    }

    function setLoading(button, loading) {
        if (!button) return;
        button.disabled = loading;
        button.classList.toggle('is-loading', loading);
        const label = button.querySelector('.button-label');
        if (!label) return;
        if (loading) {
            button.dataset.originalLabel = label.textContent;
            label.textContent = 'Đang xử lý…';
        } else if (button.dataset.originalLabel) {
            label.textContent = button.dataset.originalLabel;
            delete button.dataset.originalLabel;
        }
    }

    function updateCartBadge(quantity) {
        const badge = document.getElementById('cartBadge');
        if (!badge) return;
        badge.textContent = String(quantity);
        badge.classList.remove('badge-bump');
        void badge.offsetWidth;
        badge.classList.add('badge-bump');
    }

    async function submitAjax(form, submitter) {
        // Gửi URL-encoded để AuthFilter đọc được CSRF token trước khi tới servlet.
        // FormData sẽ tạo multipart/form-data và request.getParameter() trong filter
        // không đọc được nếu servlet không bật multipart parsing.
        const formData = new URLSearchParams(new FormData(form));
        formData.set('ajax', '1');
        if (submitter && submitter.name) {
            formData.set(submitter.name, submitter.value);
        }
        // Không dùng form.action: input hidden name="action" có thể che thuộc tính này.
        const endpoint = new URL(form.getAttribute('action'), window.location.href).href;
        const response = await fetch(endpoint, {
            method: 'POST',
            body: formData,
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
        const payload = await response.json();
        if (payload.loginRequired && payload.loginUrl) {
            window.location.assign(payload.loginUrl);
            return null;
        }
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || 'Không thể thực hiện thao tác.');
        }
        return payload;
    }

    document.querySelectorAll('.js-cart-form').forEach(function (form) {
        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            const submitter = event.submitter || form.querySelector('[type="submit"]');
            const intent = submitter && submitter.dataset.cartIntent;
            setLoading(submitter, true);
            try {
                const payload = await submitAjax(form, submitter);
                if (!payload) return;
                updateCartBadge(payload.cartTotalQuantity);
                showToast(payload.message, 'success');
                if (intent === 'checkout') {
                    window.location.assign(form.dataset.checkoutUrl || '/order');
                }
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                setLoading(submitter, false);
            }
        });
    });

    const favoriteForm = document.querySelector('.js-favorite-form');
    if (favoriteForm) {
        favoriteForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            const button = favoriteForm.querySelector('button[type="submit"]');
            button.disabled = true;
            button.classList.add('is-loading');
            try {
                const payload = await submitAjax(favoriteForm, button);
                if (!payload) return;
                favoriteForm.querySelector('input[name="action"]').value = payload.nextAction;
                favoriteForm.querySelector('.favorite-label').textContent = payload.label;
                favoriteForm.querySelector('.favorite-icon').textContent = payload.favorite ? '♥' : '♡';
                button.classList.toggle('is-favorite', payload.favorite);
                button.setAttribute('aria-pressed', String(payload.favorite));
                showToast(payload.message, 'success');
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                button.disabled = false;
                button.classList.remove('is-loading');
            }
        });
    }

    document.querySelectorAll('.product-thumbnail').forEach(function (thumbnail) {
        thumbnail.addEventListener('click', function () {
            const mainImage = document.getElementById('mainProductImage');
            if (!mainImage) return;
            mainImage.src = thumbnail.dataset.image;
            mainImage.alt = thumbnail.dataset.alt || '';
            document.querySelectorAll('.product-thumbnail').forEach(function (item) {
                item.classList.remove('active');
            });
            thumbnail.classList.add('active');
        });
    });
})();
