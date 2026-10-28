(function () {
    'use strict';

    const page = document.getElementById('cartPage');
    if (!page) return;

    const money = new Intl.NumberFormat('vi-VN');
    const notice = document.getElementById('cartNotice');
    const confirmModal = document.getElementById('cartConfirmModal');
    const confirmDialog = confirmModal && confirmModal.querySelector('.cart-confirm-dialog');
    const confirmTitle = document.getElementById('cartConfirmTitle');
    const confirmMessage = document.getElementById('cartConfirmMessage');
    const confirmSubmit = document.getElementById('cartConfirmSubmit');
    let pendingForm = null;
    let previousFocus = null;

    function showNotice(message, error) {
        if (!notice) return;
        notice.hidden = false;
        notice.textContent = message;
        notice.className = error ? 'notice-error' : 'notice-ok';
    }

    function updateBadge(quantity) {
        const badge = document.getElementById('cartBadge');
        if (badge) badge.textContent = String(quantity);
    }

    function renderEmptyCart() {
        const content = document.getElementById('cartContent');
        if (!content) return;
        const empty = document.createElement('div');
        empty.className = 'empty-state';
        const text = document.createElement('p');
        text.textContent = 'Chưa có sản phẩm nào trong giỏ hàng.';
        const link = document.createElement('a');
        link.className = 'button-primary';
        link.href = page.dataset.productsUrl;
        link.style.marginTop = '16px';
        link.textContent = 'Xem sản phẩm';
        empty.append(text, link);
        content.replaceWith(empty);
    }

    function closeConfirm() {
        if (!confirmModal || confirmModal.hidden) return;
        confirmModal.hidden = true;
        document.body.classList.remove('modal-open');
        pendingForm = null;
        if (previousFocus) previousFocus.focus();
        previousFocus = null;
    }

    function openConfirm(form) {
        if (!confirmModal) return;
        pendingForm = form;
        previousFocus = document.activeElement;
        if (confirmTitle) confirmTitle.textContent = form.dataset.confirmTitle || 'Xóa sản phẩm?';
        if (confirmMessage) confirmMessage.textContent = form.dataset.confirm;
        confirmModal.hidden = false;
        document.body.classList.add('modal-open');
        if (confirmDialog) confirmDialog.focus();
    }

    async function mutateCart(form) {
        const button = form.querySelector('button[type="submit"]');
        if (button) button.disabled = true;
        try {
            const body = new URLSearchParams(new FormData(form));
            body.set('ajax', '1');
            const response = await fetch(form.getAttribute('action'), {
                method: 'POST', body: body, credentials: 'same-origin',
                headers: {'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest'}
            });
            const data = await response.json();
            if (!response.ok || !data.success) {
                throw new Error(data.message || 'Không thể cập nhật giỏ hàng.');
            }

            const action = body.get('action');
            const id = body.get('shopProductId');
            if (action === 'update') {
                const line = document.querySelector('[data-cart-line="' + CSS.escape(id) + '"]');
                const subtotal = line && line.querySelector('[data-line-subtotal]');
                if (subtotal) subtotal.textContent = money.format(data.lineSubtotal) + 'đ';
            } else if (action === 'remove') {
                const line = document.querySelector('[data-cart-line="' + CSS.escape(id) + '"]');
                if (line) line.remove();
            } else if (action === 'clear') {
                document.querySelectorAll('[data-cart-line]').forEach(function (line) { line.remove(); });
            }

            updateBadge(data.cartTotalQuantity);
            const heading = document.getElementById('cartQuantityHeading');
            if (heading) heading.textContent = data.cartTotalQuantity + ' sản phẩm';
            const subtotal = document.getElementById('cartSubtotal');
            if (subtotal) subtotal.textContent = money.format(data.cartSubtotal) + 'đ';
            showNotice(data.message, false);
            if (data.cartTotalQuantity === 0) renderEmptyCart();
        } catch (error) {
            showNotice(error.message || 'Không thể cập nhật giỏ hàng.', true);
        } finally {
            if (button) button.disabled = false;
        }
    }

    document.querySelectorAll('.js-cart-mutation').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            if (form.dataset.confirm) {
                openConfirm(form);
                return;
            }
            mutateCart(form);
        });
    });

    if (confirmModal) {
        confirmModal.querySelectorAll('[data-confirm-cancel]').forEach(function (button) {
            button.addEventListener('click', closeConfirm);
        });
    }

    if (confirmSubmit) {
        confirmSubmit.addEventListener('click', function () {
            const form = pendingForm;
            closeConfirm();
            if (form) mutateCart(form);
        });
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && confirmModal && !confirmModal.hidden) {
            closeConfirm();
        }
    });
})();
