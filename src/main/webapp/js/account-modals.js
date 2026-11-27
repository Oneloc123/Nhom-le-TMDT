(function () {
    'use strict';

    let generatedTriggerId = 0;

    function findDialog(selector) {
        if (!selector) return null;
        try { return document.querySelector(selector); } catch (error) { return null; }
    }

    function openDialog(dialog, trigger) {
        if (!dialog) return;
        if (trigger && !trigger.id) trigger.id = 'account-modal-trigger-' + (++generatedTriggerId);
        if (typeof dialog.showModal === 'function') {
            if (!dialog.open) dialog.showModal();
        } else {
            dialog.setAttribute('open', '');
        }
        dialog.dataset.returnFocusId = trigger && trigger.id ? trigger.id : '';
        const focusTarget = dialog.querySelector('[data-modal-autofocus], input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([data-modal-close]):not([data-help-toggle])');
        if (focusTarget) window.setTimeout(function () { focusTarget.focus({ preventScroll: true }); }, 0);
    }

    function closeDialog(dialog) {
        if (!dialog) return;
        const focusId = dialog.dataset.returnFocusId;
        if (typeof dialog.close === 'function' && dialog.open) dialog.close();
        else dialog.removeAttribute('open');
        if (focusId) {
            const trigger = document.getElementById(focusId);
            if (trigger) trigger.focus({ preventScroll: true });
        }
    }

    if (!window.__pkAccountModalsClickBound) {
        window.__pkAccountModalsClickBound = true;
        document.addEventListener('click', function (event) {
        const closeButton = event.target.closest('[data-modal-close]');
        if (closeButton) {
            closeDialog(closeButton.closest('dialog'));
            return;
        }
        const helpToggle = event.target.closest('[data-help-toggle]');
        if (helpToggle) {
            const panel = document.getElementById(helpToggle.getAttribute('aria-controls'));
            if (!panel) return;
            const willShow = panel.hidden;
            panel.hidden = !willShow;
            helpToggle.setAttribute('aria-expanded', String(willShow));
            helpToggle.textContent = willShow ? 'Ẩn hướng dẫn' : 'Xem hướng dẫn';
            return;
        }
        const opener = event.target.closest('[data-modal-open]');
        if (!opener) return;
        const dialog = findDialog(opener.getAttribute('data-modal-open'));
        if (opener.hasAttribute('data-address-create')) {
            const form = dialog && dialog.querySelector('[data-address-editor-form]');
            if (form) {
                form.reset();
                const id = form.querySelector('[name="addressId"]');
                if (id) id.value = '';
                const action = form.querySelector('[name="action"]');
                if (action) action.value = 'save';
                ['recipientName', 'recipientPhone', 'addressLine', 'deliveryNote'].forEach(function (name) {
                    const field = form.querySelector('[name="' + name + '"]');
                    if (field) field.value = '';
                });
                const label = form.querySelector('[name="label"]');
                if (label) label.value = 'Nhà riêng';
                const defaultCheckbox = form.querySelector('[name="isDefault"]');
                if (defaultCheckbox) defaultCheckbox.checked = false;
                if (form.clearAddressSelection) form.clearAddressSelection();
                const heading = dialog.querySelector('[data-address-modal-title]');
                if (heading) heading.textContent = 'Thêm địa chỉ mới';
            }
        }
        if (opener.hasAttribute('data-address-edit')) {
            const card = opener.closest('[data-address-card]');
            const form = dialog && dialog.querySelector('[data-address-editor-form]');
            if (card && form) {
                const values = {
                    addressId: card.dataset.addressId || '',
                    recipientName: card.dataset.recipientName || '',
                    recipientPhone: card.dataset.recipientPhone || '',
                    label: card.dataset.addressLabel || 'Nhà riêng',
                    province: card.dataset.province || '',
                    provinceCode: card.dataset.provinceCode || '',
                    district: card.dataset.district || '',
                    ward: card.dataset.ward || '',
                    wardCode: card.dataset.wardCode || '',
                    addressLine: card.dataset.addressLine || '',
                    deliveryNote: card.dataset.deliveryNote || '',
                    isDefault: card.dataset.isDefault === 'true'
                };
                Object.keys(values).forEach(function (name) {
                    const field = form.querySelector('[name="' + name + '"]');
                    if (!field) return;
                    if (field.type === 'checkbox') field.checked = values[name];
                    else if (field.type !== 'hidden' || !field.hasAttribute('data-province-value') && !field.hasAttribute('data-ward-value')) field.value = values[name];
                });
                const action = form.querySelector('[name="action"]');
                if (action) action.value = 'save';
                const heading = dialog.querySelector('[data-address-modal-title]');
                if (heading) heading.textContent = 'Chỉnh sửa địa chỉ';
                if (form.selectAddressSelection) form.selectAddressSelection(values);
            }
        }
        if (opener.hasAttribute('data-address-default')) {
            const card = opener.closest('[data-address-card]');
            const dialog = findDialog(opener.getAttribute('data-modal-open'));
            const form = dialog && dialog.querySelector('[data-address-default-form]');
            if (form && card) {
                const id = form.querySelector('[name="addressId"]');
                if (id) id.value = card.dataset.addressId || '';
                const label = dialog.querySelector('[data-address-default-label]');
                if (label) label.textContent = (card.dataset.addressLabel || 'địa chỉ') + ' — ' + (card.dataset.recipientName || '') + ' (' + (card.dataset.addressLine || '') + ')';
            }
        }
        if (opener.hasAttribute('data-address-delete')) {
            const card = opener.closest('[data-address-card]');
            const dialog = findDialog(opener.getAttribute('data-modal-open'));
            const form = dialog && dialog.querySelector('[data-address-delete-form]');
            if (form && card) {
                const id = form.querySelector('[name="addressId"]');
                if (id) id.value = card.dataset.addressId || '';
                const label = dialog.querySelector('[data-address-delete-label]');
                if (label) label.textContent = (card.dataset.addressLabel || 'địa chỉ') + ' — ' + (card.dataset.recipientName || '') + ' (' + (card.dataset.addressLine || '') + ')';
            }
        }
        openDialog(dialog, opener);
    });
    }

    function initAccountModalElements() {
        document.querySelectorAll('dialog[data-account-modal]').forEach(function (dialog) {
            if (dialog.dataset.modalBound === '1') return;
            dialog.dataset.modalBound = '1';
            dialog.addEventListener('click', function (event) {
                if (event.target === dialog) closeDialog(dialog);
            });
            dialog.addEventListener('close', function () {
                const focusId = dialog.dataset.returnFocusId;
                if (focusId) {
                    const trigger = document.getElementById(focusId);
                    if (trigger) trigger.focus({ preventScroll: true });
                }
            });
        });

        const addressModal = document.getElementById('addressEditorModal');
        const addressForm = addressModal && addressModal.querySelector('[data-address-editor-form]');
        if (addressForm && addressForm.dataset.submitBound !== '1') {
            addressForm.dataset.submitBound = '1';
            addressForm.addEventListener('submit', function (event) {
                if (!addressForm.checkValidity()) {
                    event.preventDefault();
                    addressForm.reportValidity();
                }
            });
        }

        document.querySelectorAll('dialog[data-modal-auto-open="true"]').forEach(function (dialog) {
            openDialog(dialog, null);
        });
    }

    window.__pkInitAccountModals = initAccountModalElements;
    initAccountModalElements();
})();
