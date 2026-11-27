(function () {
    'use strict';

    const toggle = document.getElementById('miniCartToggle');
    const panel = document.getElementById('miniCartPanel');
    const itemsNode = document.getElementById('miniCartItems');
    const subtotalNode = document.getElementById('miniCartSubtotal');
    const countNode = document.getElementById('miniCartCountText');
    const closeButton = document.getElementById('miniCartClose');
    const csrfNode = document.getElementById('miniCartCsrfToken');
    if (!toggle || !panel || !itemsNode || !subtotalNode) return;

    const base = toggle.dataset.contextPath || '';
    const money = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });
    let loading = false;
    let lastLoadedAt = 0;

    function updateBadge(quantity) {
        const badge = document.getElementById('cartBadge');
        if (!badge || typeof quantity !== 'number') return;
        badge.textContent = String(quantity);
        badge.classList.remove('badge-bump');
        void badge.offsetWidth;
        badge.classList.add('badge-bump');
        if (countNode) countNode.textContent = quantity + ' sản phẩm trong giỏ';
    }

    function create(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function renderEmpty() {
        itemsNode.replaceChildren();
        const empty = create('div', 'mini-cart-empty');
        empty.append(create('div', '', '🛍️'));
        empty.append(create('strong', '', 'Giỏ hàng đang trống'));
        empty.append(create('div', '', 'Hãy chọn sản phẩm bạn yêu thích để bắt đầu mua sắm.'));
        itemsNode.append(empty);
        subtotalNode.textContent = '0đ';
    }

    function renderError(message) {
        itemsNode.replaceChildren(create('div', 'mini-cart-error', message));
    }

    function renderItems(data) {
        updateBadge(Number(data.cartTotalQuantity || 0));
        subtotalNode.textContent = money.format(Number(data.cartSubtotal || 0)) + 'đ';
        itemsNode.replaceChildren();
        if (!Array.isArray(data.items) || data.items.length === 0) {
            renderEmpty();
            return;
        }
        data.items.slice(0, 30).forEach(function (item) {
            const row = create('article', 'mini-cart-row');
            const allowedClasses = ['warm', 'primary', 'dark', 'teal', 'sky', 'pink', 'cool', 'brown', 'violet', 'green'];
            const visualClass = allowedClasses.includes(item.visualClass) ? item.visualClass : 'warm';
            const thumb = create('div', 'mini-cart-thumb ' + visualClass, (item.name || 'Sản phẩm').slice(0, 22));
            thumb.setAttribute('aria-hidden', 'true');
            const details = create('div', 'mini-cart-product');
            const link = create('a', '', item.name || 'Sản phẩm');
            link.href = item.detailUrl || (base + '/products');
            link.title = item.name || 'Sản phẩm';
            const shop = create('div', 'mini-cart-shop', item.shopName || 'Xưởng in');
            const bottom = create('div', 'mini-cart-row-bottom');
            bottom.append(create('span', 'mini-cart-qty', 'SL: ' + Number(item.quantity || 0)));
            bottom.append(create('strong', 'mini-cart-price', money.format(Number(item.subtotal || 0)) + 'đ'));
            const remove = create('button', 'mini-cart-remove', 'Xóa');
            remove.type = 'button';
            remove.dataset.removeShopProductId = String(item.shopProductId || '');
            remove.setAttribute('aria-label', 'Xóa ' + (item.name || 'sản phẩm') + ' khỏi giỏ hàng');
            bottom.append(remove);
            details.append(link, shop, bottom);
            row.append(thumb, details);
            itemsNode.append(row);
        });
        if (data.items.length > 30) {
            itemsNode.append(create('div', 'mini-cart-loading', 'Còn ' + (data.items.length - 30) + ' dòng khác trong giỏ hàng.'));
        }
    }

    async function refresh(force) {
        if (loading) return;
        if (!force && Date.now() - lastLoadedAt < 4500) return;
        loading = true;
        itemsNode.replaceChildren(create('div', 'mini-cart-loading', 'Đang tải giỏ hàng…'));
        try {
            const response = await fetch(base + '/mini-cart-data', {
                method: 'GET', credentials: 'same-origin', cache: 'no-store',
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
            });
            const data = await response.json();
            if (!response.ok || !data.success) throw new Error(data.message || 'Không thể tải giỏ hàng.');
            renderItems(data);
            lastLoadedAt = Date.now();
        } catch (error) {
            renderError(error.message || 'Không thể tải giỏ hàng. Vui lòng thử lại.');
        } finally {
            loading = false;
        }
    }

    async function removeItem(button) {
        const shopProductId = button.dataset.removeShopProductId;
        const csrfToken = csrfNode ? csrfNode.value : '';
        if (!shopProductId || !csrfToken) {
            itemsNode.prepend(create('div', 'mini-cart-action-error', 'Không thể xác minh thao tác. Vui lòng tải lại trang.'));
            return;
        }
        itemsNode.querySelectorAll('.mini-cart-action-error').forEach(function (node) { node.remove(); });
        const originalText = button.textContent;
        button.disabled = true;
        button.textContent = 'Đang xóa…';
        try {
            const response = await fetch(base + '/cart', {
                method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new URLSearchParams({ csrfToken: csrfToken, action: 'remove', shopProductId: shopProductId })
            });
            const data = await response.json();
            if (!response.ok || !data.success) throw new Error(data.message || 'Không thể xóa sản phẩm. Vui lòng thử lại.');
            updateBadge(Number(data.cartTotalQuantity || 0));
            subtotalNode.textContent = money.format(Number(data.cartSubtotal || 0)) + 'đ';
            lastLoadedAt = 0;
            window.dispatchEvent(new CustomEvent('printkraft:cart-updated', {
                detail: { cartTotalQuantity: Number(data.cartTotalQuantity || 0) }
            }));
        } catch (error) {
            button.disabled = false;
            button.textContent = originalText;
            itemsNode.prepend(create('div', 'mini-cart-action-error', error.message || 'Không thể xóa sản phẩm. Vui lòng thử lại.'));
        }
    }

    function openPanel() {
        panel.hidden = false;
        toggle.setAttribute('aria-expanded', 'true');
        refresh(false);
    }

    function closePanel(returnFocus) {
        panel.hidden = true;
        toggle.setAttribute('aria-expanded', 'false');
        if (returnFocus) toggle.focus();
    }

    itemsNode.addEventListener('click', function (event) {
        const button = event.target.closest('[data-remove-shop-product-id]');
        if (button) removeItem(button);
    });
    toggle.addEventListener('click', function () {
        if (panel.hidden) openPanel();
        else closePanel(false);
    });
    if (closeButton) closeButton.addEventListener('click', function () { closePanel(true); });
    document.addEventListener('click', function (event) {
        if (!panel.hidden && !event.target.closest('.mini-cart-wrap')) closePanel(false);
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !panel.hidden) closePanel(true);
    });
    window.addEventListener('printkraft:cart-updated', function (event) {
        if (event.detail && typeof event.detail.cartTotalQuantity === 'number') {
            updateBadge(event.detail.cartTotalQuantity);
        }
        lastLoadedAt = 0;
        if (!panel.hidden) refresh(true);
    });
})();
