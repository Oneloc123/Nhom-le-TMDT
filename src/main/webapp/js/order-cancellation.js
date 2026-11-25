(() => {
    const form = document.getElementById('cancelOrderForm');
    const dialog = document.getElementById('cancelOrderDialog');
    const reason = document.getElementById('cancelReason');
    if (!form || !dialog || !reason) return;

    const openButton = form.querySelector('[data-open-cancel-dialog]');
    const closeButtons = form.querySelectorAll('[data-close-cancel-dialog]');
    const submitButton = form.querySelector('[data-confirm-cancel]');
    const counter = form.querySelector('[data-reason-count]');
    const suggestions = form.querySelectorAll('[data-reason-suggestion]');

    const updateCounter = () => {
        if (counter) counter.textContent = String(reason.value.length);
    };

    const openDialog = () => {
        if (typeof dialog.showModal === 'function') {
            if (!dialog.open) dialog.showModal();
        } else {
            dialog.setAttribute('open', '');
        }
        reason.focus();
    };

    const closeDialog = () => {
        if (typeof dialog.close === 'function' && dialog.open) {
            dialog.close();
        } else {
            dialog.removeAttribute('open');
        }
        if (openButton) openButton.focus();
    };

    if (openButton) openButton.addEventListener('click', openDialog);
    closeButtons.forEach((button) => button.addEventListener('click', closeDialog));
    dialog.addEventListener('click', (event) => {
        if (event.target === dialog) closeDialog();
    });

    suggestions.forEach((button) => {
        button.addEventListener('click', () => {
            reason.value = button.getAttribute('data-reason-suggestion') || '';
            reason.setCustomValidity('');
            updateCounter();
            reason.focus();
        });
    });

    reason.addEventListener('input', () => {
        reason.setCustomValidity('');
        updateCounter();
    });

    form.addEventListener('submit', (event) => {
        const cleanReason = reason.value.trim();
        if (!cleanReason) {
            event.preventDefault();
            reason.setCustomValidity('Vui lòng nhập lý do hủy đơn hàng.');
            reason.reportValidity();
            reason.focus();
            return;
        }
        if (cleanReason.length > 500) {
            event.preventDefault();
            reason.setCustomValidity('Lý do hủy không được vượt quá 500 ký tự.');
            reason.reportValidity();
            reason.focus();
            return;
        }
        reason.value = cleanReason;
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = 'Đang gửi yêu cầu...';
        }
        form.setAttribute('aria-busy', 'true');
    });

    updateCounter();
    if (form.dataset.reopen === 'true' && typeof dialog.showModal === 'function') {
        dialog.showModal();
        reason.focus();
    }
})();
