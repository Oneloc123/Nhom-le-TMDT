(() => {
    'use strict';

    // Hiện/ẩn mật khẩu mà không gửi dữ liệu ra ngoài.
    document.querySelectorAll('[data-password-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            const targetId = button.getAttribute('data-password-toggle');
            const input = document.getElementById(targetId);
            if (!input) return;
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            button.textContent = show ? 'Ẩn' : 'Hiện';
            button.setAttribute('aria-label', `${show ? 'Ẩn' : 'Hiện'} ${button.dataset.label || 'mật khẩu'}`);
        });
    });

    // Đánh giá sơ bộ độ mạnh mật khẩu ngay trên trình duyệt.
    const strengthInput = document.querySelector('[data-password-strength]');
    const strengthBar = document.querySelector('[data-strength-bar]');
    const strengthLabel = document.querySelector('[data-strength-label]');
    if (strengthInput && strengthBar && strengthLabel) {
        const updateStrength = () => {
            const value = strengthInput.value;
            let score = 0;
            if (value.length >= 8) score++;
            if (value.length >= 12) score++;
            if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
            if (/\d/.test(value)) score++;
            if (/[^A-Za-z0-9]/.test(value)) score++;
            const percent = value.length === 0 ? 0 : Math.min(100, score * 20);
            strengthBar.style.width = `${percent}%`;
            strengthBar.dataset.level = value.length === 0 ? 'empty' : score <= 2 ? 'weak' : score <= 3 ? 'medium' : 'strong';
            strengthLabel.textContent = value.length === 0
                ? 'Độ mạnh mật khẩu sẽ hiển thị tại đây.'
                : score <= 2 ? 'Mật khẩu còn yếu — hãy tăng độ dài và kết hợp nhiều loại ký tự.'
                    : score <= 3 ? 'Mật khẩu ở mức trung bình — có thể tăng độ dài để an toàn hơn.'
                        : 'Mật khẩu khá mạnh.';
        };
        strengthInput.addEventListener('input', updateStrength);
        updateStrength();
    }

    // Báo lỗi nếu hai mật khẩu không khớp trước khi gửi form.
    document.querySelectorAll('[data-password-form]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            const next = form.querySelector('[data-password-strength]');
            const confirm = form.querySelector('[data-password-confirm]');
            if (next && confirm && next.value !== confirm.value) {
                event.preventDefault();
                confirm.setCustomValidity('Mật khẩu xác nhận không khớp.');
                confirm.reportValidity();
                confirm.focus();
                return;
            }
            if (confirm) confirm.setCustomValidity('');
        });
    });
    document.querySelectorAll('[data-password-confirm]').forEach((input) => {
        input.addEventListener('input', () => input.setCustomValidity(''));
    });

    // Mở biểu mẫu sửa đúng địa chỉ; hỗ trợ nhiều thẻ độc lập.
    document.querySelectorAll('[data-edit-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            const id = button.getAttribute('data-edit-toggle');
            const form = document.querySelector(`[data-edit-form="${CSS.escape(id)}"]`);
            if (!form) return;
            const opening = form.hidden;
            form.hidden = !opening;
            button.setAttribute('aria-expanded', String(opening));
            button.textContent = opening ? 'Đóng chỉnh sửa' : 'Chỉnh sửa';
            if (opening) form.querySelector('input:not([type="hidden"])')?.focus();
        });
    });
    document.querySelectorAll('[data-edit-cancel]').forEach((button) => {
        button.addEventListener('click', () => {
            const id = button.getAttribute('data-edit-cancel');
            const form = document.querySelector(`[data-edit-form="${CSS.escape(id)}"]`);
            const toggle = document.querySelector(`[data-edit-toggle="${CSS.escape(id)}"]`);
            if (form) form.hidden = true;
            if (toggle) {
                toggle.setAttribute('aria-expanded', 'false');
                toggle.textContent = 'Chỉnh sửa';
            }
        });
    });

    // Xác nhận trước thao tác xóa địa chỉ.
    document.querySelectorAll('form[data-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (!window.confirm(form.getAttribute('data-confirm'))) event.preventDefault();
        });
    });

    // Lọc nhanh danh sách địa chỉ, không thay đổi dữ liệu trên máy chủ.
    const search = document.getElementById('addressSearch');
    const noResults = document.getElementById('addressNoResults');
    if (search) {
        search.addEventListener('input', () => {
            const query = search.value.trim().toLocaleLowerCase('vi');
            const cards = [...document.querySelectorAll('[data-address-card]')];
            let visible = 0;
            cards.forEach((card) => {
                const content = (card.getAttribute('data-search') || card.textContent).toLocaleLowerCase('vi');
                const matches = content.includes(query);
                card.hidden = !matches;
                if (matches) visible++;
            });
            if (noResults) noResults.hidden = visible > 0 || cards.length === 0;
        });
    }
})();
