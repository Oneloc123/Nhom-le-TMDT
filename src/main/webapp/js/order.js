(function () {
    'use strict';

    const manualPanel = document.getElementById('manualAddress');
    if (!manualPanel) return;

    const addressChoices = Array.from(document.querySelectorAll('input[name="addressId"]'));
    const manualFields = Array.from(manualPanel.querySelectorAll('input, textarea'));

    function updateManualAddress() {
        const selected = addressChoices.find(function (choice) { return choice.checked; });
        const manual = !selected || selected.value === '';
        manualPanel.hidden = !manual;
        manualFields.forEach(function (field) {
            field.required = manual;
            field.disabled = !manual;
        });
    }

    addressChoices.forEach(function (choice) {
        choice.addEventListener('change', updateManualAddress);
    });
    updateManualAddress();
})();
