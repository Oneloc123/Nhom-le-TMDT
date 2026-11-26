(() => {
    'use strict';

    function initAccountPage() {
        // Hiện/ẩn mật khẩu mà không gửi dữ liệu ra ngoài.
        document.querySelectorAll('[data-password-toggle]').forEach((button) => {
            if (button.dataset.bound === '1') return;
            button.dataset.bound = '1';
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
        if (strengthInput && strengthBar && strengthLabel && strengthInput.dataset.bound !== '1') {
            strengthInput.dataset.bound = '1';
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
            if (form.dataset.bound === '1') return;
            form.dataset.bound = '1';
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
            if (input.dataset.bound === '1') return;
            input.dataset.bound = '1';
            input.addEventListener('input', () => input.setCustomValidity(''));
        });

        // Mở biểu mẫu sửa đúng địa chỉ; hỗ trợ nhiều thẻ độc lập.
        document.querySelectorAll('[data-edit-toggle]').forEach((button) => {
            if (button.dataset.bound === '1') return;
            button.dataset.bound = '1';
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
            if (button.dataset.bound === '1') return;
            button.dataset.bound = '1';
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
            if (form.dataset.bound === '1') return;
            form.dataset.bound = '1';
            form.addEventListener('submit', (event) => {
                if (!window.confirm(form.getAttribute('data-confirm'))) event.preventDefault();
            });
        });

        // Lọc nhanh danh sách địa chỉ, không thay đổi dữ liệu trên máy chủ.
        const search = document.getElementById('addressSearch');
        const noResults = document.getElementById('addressNoResults');
        if (search && search.dataset.bound !== '1') {
            search.dataset.bound = '1';
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

        // Xem trước ảnh đại diện trước khi gửi lên máy chủ.
        document.querySelectorAll('[data-avatar-preview]').forEach((input) => {
            if (input.dataset.bound === '1') return;
            input.dataset.bound = '1';
            input.addEventListener('change', () => {
                const file = input.files && input.files[0];
                const preview = document.getElementById('avatarPreview');
                const currentAvatar = document.querySelector('[data-avatar-current]');
                if (!preview) return;
                if (!file) {
                    input.setCustomValidity('');
                    preview.hidden = true;
                    preview.removeAttribute('src');
                    if (currentAvatar) currentAvatar.hidden = false;
                    return;
                }
                if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 3 * 1024 * 1024) {
                    input.setCustomValidity('Chỉ nhận ảnh PNG/JPEG tối đa 3 MB.');
                    input.reportValidity(); input.value = ''; preview.hidden = true;
                    if (currentAvatar) currentAvatar.hidden = false;
                    return;
                }
                input.setCustomValidity('');
                preview.src = URL.createObjectURL(file);
                preview.hidden = false;
                if (currentAvatar) currentAvatar.hidden = true;
                preview.onload = () => URL.revokeObjectURL(preview.src);
            });
        });
    }

    window.__pkInitAccountPage = initAccountPage;
    initAccountPage();
})();

if (!window.__pkAccountCopyBound) {
    window.__pkAccountCopyBound = true;
    document.addEventListener('click', async event => {
        const button = event.target.closest('[data-copy-address]');
        if (!button) return;
        const value = button.getAttribute('data-copy-address') || '';
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(value);
            } else {
                const helper = document.createElement('textarea'); helper.value = value; helper.style.position = 'fixed'; helper.style.opacity = '0';
                document.body.appendChild(helper); helper.select();
                const copied = document.execCommand('copy'); helper.remove(); if (!copied) throw new Error('copy failed');
            }
            const previous = button.textContent; button.textContent = 'Đã sao chép'; button.setAttribute('aria-live', 'polite');
            window.setTimeout(() => { button.textContent = previous; }, 1600);
        } catch (error) {
            window.prompt('Sao chép địa chỉ này:', value);
        }
    });
}
