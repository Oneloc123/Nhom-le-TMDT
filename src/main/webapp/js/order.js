(function () {
    'use strict';

    const form = document.getElementById('checkoutForm');
    const manualPanel = document.getElementById('manualAddress');
    if (!form || !manualPanel) return;

    const addressChoices = Array.from(document.querySelectorAll('input[name="addressId"]'));
    const manualFields = Array.from(manualPanel.querySelectorAll('input, select, textarea'))
        .filter(function (field) { return !field.hasAttribute('data-province-fallback') && !field.hasAttribute('data-ward-fallback'); });

    function updateManualAddress(shouldFocus) {
        const selected = addressChoices.find(function (choice) { return choice.checked; });
        const manual = !selected || selected.value === '';
        manualPanel.hidden = !manual;
        manualPanel.setAttribute('aria-hidden', String(!manual));
        addressChoices.forEach(function (choice) {
            const card = choice.closest('.address-card');
            if (card) card.classList.toggle('is-selected', choice.checked);
        });
        manualFields.forEach(function (field) {
            field.required = manual && !field.hidden && field.type !== 'hidden';
            field.disabled = !manual || field.hidden;
        });
        manualPanel.querySelectorAll('[data-province-fallback], [data-ward-fallback]').forEach(function (field) {
            field.disabled = !manual || field.hidden;
            field.required = manual && !field.hidden;
        });
        if (manual && shouldFocus && manualFields[0]) {
            window.requestAnimationFrame(function () { manualFields[0].focus(); });
        }
    }

    const confirmedFee = form.elements.namedItem('confirmedShippingFee');
    const submit = form.querySelector('.checkout-submit');
    const status = document.getElementById('shippingQuoteStatus');
    const feeLabel = document.getElementById('checkoutShippingFee');
    const totalLabel = document.getElementById('checkoutTotal');
    const retry = document.getElementById('retryShippingQuote');
    const subtotal = Number(form.dataset.subtotal);
    const currency = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });
    const quoteUrl = new URL(form.action);
    quoteUrl.searchParams.set('shippingQuote', '1');
    let timer;
    let pending;

    async function fetchQuote(body, request) {
        try {
            const response = await fetch(quoteUrl, {
                method: 'POST',
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                body,
                signal: request.signal
            });
            const data = await response.json();
            if (request.signal.aborted) return;
            if (!response.ok || !Number.isSafeInteger(data.fee) || data.fee < 0) {
                throw new Error(data.message || 'GHN chưa thể tính phí lúc này.');
            }
            confirmedFee.value = String(data.fee);
            feeLabel.textContent = currency.format(data.fee) + ' đ';
            totalLabel.textContent = currency.format(subtotal + data.fee) + ' đ';
            status.textContent = 'Đã cập nhật phí vận chuyển GHN.';
            submit.disabled = false;
        } catch (error) {
            if (request.signal.aborted) return;
            status.textContent = error instanceof SyntaxError || error instanceof TypeError
                ? 'Không thể tải phí GHN. Vui lòng thử lại.' : error.message;
            retry.hidden = false;
        }
    }

    function scheduleQuote() {
        clearTimeout(timer);
        if (pending) pending.abort();
        confirmedFee.value = '';
        submit.disabled = true;
        retry.hidden = true;
        feeLabel.textContent = '—';
        totalLabel.textContent = '—';
        const fields = new FormData(form);
        const body = new URLSearchParams({ csrfToken: fields.get('csrfToken') });
        const addressId = fields.get('addressId');
        if (addressId) {
            body.set('addressId', addressId);
        } else {
            const required = ['province', 'ward', 'recipientName', 'phone', 'addressLine'];
            if (required.some(function (name) { return !(fields.get(name) || '').trim(); })) {
                status.textContent = 'Điền đầy đủ thông tin nhận hàng để tính phí GHN.';
                return;
            }
            required.forEach(function (name) { body.set(name, fields.get(name)); });
        }
        status.textContent = 'Đang tính phí vận chuyển GHN…';
        const request = new AbortController();
        pending = request;
        timer = setTimeout(function () { fetchQuote(body, request); }, 300);
    }

    addressChoices.forEach(function (choice) {
        choice.addEventListener('change', function () {
            updateManualAddress(true);
            scheduleQuote();
        });
    });
    manualPanel.addEventListener('input', function (event) {
        if (event.target instanceof HTMLInputElement) scheduleQuote();
    });
    manualPanel.addEventListener('change', function (event) {
        if (event.target instanceof HTMLSelectElement) scheduleQuote();
    });
    form.addEventListener('submit', function (event) {
        if (!confirmedFee.value) event.preventDefault();
    });
    retry.addEventListener('click', scheduleQuote);
    updateManualAddress(false);
    scheduleQuote();
    if (manualPanel.addressPickerReady) {
        manualPanel.addressPickerReady.then(function () {
            if (!manualPanel.hidden) scheduleQuote();
        });
    }
})();
