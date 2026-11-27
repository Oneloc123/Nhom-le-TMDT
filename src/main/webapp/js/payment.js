document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('[data-payment-form]');
    const actionText = document.getElementById('paymentActionText');
    const processNote = document.getElementById('paymentProcessNote');
    if (!form || !actionText) return;

    const updateAction = () => {
        const method = form.querySelector('input[name="method"]:checked')?.value;
        actionText.textContent = method === 'VNPay'
            ? 'Thanh toán qua VNPAY'
            : method === 'ZaloPay'
                ? 'Thanh toán qua ZaloPay'
                : 'Xác nhận đơn COD';
        form.querySelectorAll('.payment-option').forEach((option) => {
            option.classList.toggle('is-selected', Boolean(option.querySelector('input[name="method"]:checked')));
        });
        if (processNote) {
            processNote.textContent = method === 'VNPay'
                ? 'Bạn sẽ được chuyển sang VNPAY. Chỉ đóng trang sau khi quay lại PrintKraft và nhận kết quả thanh toán.'
                : method === 'ZaloPay'
                    ? 'Bạn sẽ được chuyển sang ZaloPay để thanh toán toàn bộ giá trị đơn hàng. PrintKraft chỉ xác nhận đơn sau khi nhận được kết quả xác thực từ ZaloPay.'
                    : 'COD không thu tiền trực tuyến. Bạn thanh toán cho đơn vị giao hàng khi nhận sản phẩm.';
        }
    };

    form.querySelectorAll('input[name="method"]').forEach((input) => {
        input.addEventListener('change', updateAction);
    });
    form.addEventListener('submit', () => {
        const submit = form.querySelector('button[type="submit"]');
        if (submit) {
            submit.disabled = true;
            submit.classList.add('is-loading');
            submit.setAttribute('aria-busy', 'true');
            actionText.textContent = 'Đang xử lý…';
        }
    });
    updateAction();
});
