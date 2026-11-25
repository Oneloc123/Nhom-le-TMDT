(function () {
    'use strict';

    const manualPanel = document.getElementById('manualAddress');
    if (!manualPanel) return;

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
            field.required = manual && field.type !== 'hidden';
            field.disabled = !manual;
        });
        manualPanel.querySelectorAll('[data-province-fallback], [data-ward-fallback]').forEach(function (field) {
            field.disabled = !manual || field.hidden;
            field.required = manual && !field.hidden;
        });
        if (manual && shouldFocus && manualFields[0]) {
            window.requestAnimationFrame(function () { manualFields[0].focus(); });
        }
    }

    addressChoices.forEach(function (choice) {
        choice.addEventListener('change', function () { updateManualAddress(true); });
    });
    updateManualAddress(false);
})();
