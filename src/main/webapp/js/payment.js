document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('.stack-form');
    const actionText = document.getElementById('paymentActionText');
    if (!form || !actionText) return;

    const updateAction = () => {
        const method = form.querySelector('input[name="method"]:checked')?.value;
        actionText.textContent = method === 'VNPay'
            ? 'Thanh toán qua VNPAY'
            : 'Xác nhận đơn COD';
    };

    form.querySelectorAll('input[name="method"]').forEach((input) => {
        input.addEventListener('change', updateAction);
    });
    form.addEventListener('submit', () => {
        const submit = form.querySelector('button[type="submit"]');
        if (submit) {
            submit.disabled = true;
            submit.classList.add('is-loading');
        }
    });
    updateAction();
});
