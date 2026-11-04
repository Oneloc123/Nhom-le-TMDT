(function () {
    'use strict';

    const manualPanel = document.getElementById('manualAddress');
    if (!manualPanel) return;

    const addressChoices = Array.from(document.querySelectorAll('input[name="addressId"]'));
    const manualFields = Array.from(manualPanel.querySelectorAll('input, textarea'));

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
            field.required = manual;
            field.disabled = !manual;
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
