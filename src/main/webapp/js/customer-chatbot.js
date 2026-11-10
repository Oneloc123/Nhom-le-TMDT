(function () {
    'use strict';

    const widget = document.getElementById('pkChatWidget');
    const launcher = document.getElementById('pkChatLauncher');
    const panel = document.getElementById('pkChatPanel');
    const closeButton = document.getElementById('pkChatClose');
    const messages = document.getElementById('pkChatMessages');
    const form = document.getElementById('pkChatForm');
    const input = document.getElementById('pkChatInput');
    if (!widget || !launcher || !panel || !messages || !form || !input) return;

    const base = widget.dataset.contextPath || '';
    const quickBar = document.getElementById('pkChatQuick');

    function normalise(value) {
        return String(value || '').toLowerCase().normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/\s+/g, ' ').trim();
    }

    function addMessage(type, text, actions) {
        const line = document.createElement('div');
        line.className = 'pk-chat-line' + (type === 'user' ? ' is-user' : '');
        const bubble = document.createElement('div');
        bubble.className = 'pk-chat-bubble';
        bubble.textContent = text;
        line.append(bubble);
        if (Array.isArray(actions) && actions.length) {
            const actionBox = document.createElement('div');
            actionBox.className = 'pk-chat-actions';
            actions.forEach(function (action) {
                const link = document.createElement('a');
                link.className = 'pk-chat-action';
                link.textContent = action.label;
                link.href = base + action.path;
                actionBox.append(link);
            });
            line.append(actionBox);
        }
        messages.append(line);
        messages.scrollTop = messages.scrollHeight;
    }

    function getAnswer(question) {
        const text = normalise(question);
        if (/^(chao|xin chao|hello|hi|hey|alo)(\b|$)/.test(text)) {
            return { text: 'Xin chào! Mình là trợ lý tự động của PrintKraft. Bạn cần tìm sản phẩm, theo dõi đơn hàng, hỏi về báo giá hay cần hỗ trợ vấn đề nào?', actions: [
                { label: 'Xem sản phẩm', path: '/products' }, { label: 'Theo dõi đơn', path: '/order-tracking' }
            ] };
        }
        if (/theo doi|van chuyen|giao hang|don hang o dau|kiem tra don|ma van don|ship|thoi gian giao/.test(text)) {
            return { text: 'Bạn mở trang Theo dõi đơn hàng và nhập mã đơn/mã theo dõi để xem trạng thái cập nhật. Nếu đơn hàng chậm hơn dự kiến, hãy chuẩn bị mã đơn khi gửi yêu cầu hỗ trợ.', actions: [
                { label: 'Theo dõi đơn hàng', path: '/order-tracking' }, { label: 'Hỗ trợ giao hàng', path: '/support' }
            ] };
        }
        if (/bao gia|bao gia|gia si|gia doanh nghiep|don hang so luong|bao gia in/.test(text)) {
            return { text: 'Với đơn hàng số lượng lớn hoặc yêu cầu riêng, bạn có thể tạo yêu cầu báo giá để trao đổi về số lượng, vật liệu, kỹ thuật in và thời gian sản xuất. Một số yêu cầu có thể cần đăng nhập.', actions: [
                { label: 'Tạo yêu cầu báo giá', path: '/quotes' }, { label: 'Xem sản phẩm', path: '/products' }
            ] };
        }
        if (/thiet ke|tuy chinh|3d|mo hinh|file in|upload file|ban ve/.test(text)) {
            return { text: 'PrintKraft hỗ trợ luồng thiết kế/in 3D và sản phẩm tùy chỉnh. Hãy mở khu vực Thiết kế 3D để xem quy trình, chuẩn bị file thiết kế cùng kích thước và yêu cầu mong muốn trước khi gửi.', actions: [
                { label: 'Mở Thiết kế 3D', path: '/design' }, { label: 'Hỏi về báo giá', path: '/quotes' }
            ] };
        }
        if (/gio hang|them vao gio|xoa san pham|dat hang|khong dat duoc/.test(text)) {
            return { text: 'Bạn có thể xem lại số lượng và tạm tính trong giỏ hàng mini ở đầu trang hoặc mở trang giỏ hàng để cập nhật/xóa sản phẩm trước khi đặt hàng. Nếu thanh toán gặp lỗi, hãy gửi yêu cầu kèm ảnh chụp thông báo lỗi.', actions: [
                { label: 'Mở giỏ hàng', path: '/cart' }, { label: 'Hỗ trợ thanh toán', path: '/support' }
            ] };
        }
        if (/doi tra|hoan tien|tra hang|hang loi|san pham loi|khieu nai|bao hanh/.test(text)) {
            return { text: 'Để yêu cầu đổi trả, hoàn tiền hoặc báo sản phẩm lỗi, hãy đăng nhập rồi tạo ticket trong mục Hỗ trợ. Mô tả tình trạng, mã đơn và đính kèm hình ảnh liên quan nếu có để nhân viên kiểm tra.', actions: [
                { label: 'Đến trang hỗ trợ', path: '/support' }, { label: 'Đơn hàng của tôi', path: '/purchase-history' }
            ] };
        }
        if (/thanh toan|vnpay|the ngan hang|loi thanh toan|khong thanh toan/.test(text)) {
            return { text: 'Khi thanh toán, hãy kiểm tra lại thông tin đơn, số tiền và trạng thái giao dịch. Nếu đã bị trừ tiền nhưng đơn chưa cập nhật, đừng thanh toán lặp lại ngay; hãy lưu mã giao dịch và liên hệ hỗ trợ.', actions: [
                { label: 'Xem đơn hàng', path: '/purchase-history' }, { label: 'Liên hệ hỗ trợ', path: '/support' }
            ] };
        }
        if (/dang nhap|dang ky|tai khoan|quen mat khau|doi mat khau/.test(text)) {
            return { text: 'Bạn có thể đăng nhập hoặc tạo tài khoản để quản lý địa chỉ, đơn hàng, yêu thích và yêu cầu báo giá. Nếu quên mật khẩu, dùng liên kết “Quên mật khẩu” trên trang đăng nhập.', actions: [
                { label: 'Đăng nhập / đăng ký', path: '/login' }, { label: 'Đơn hàng', path: '/purchase-history' }
            ] };
        }
        if (/san pham|ao thun|coc|poster|mua gi|danh muc|xem hang/.test(text)) {
            return { text: 'Bạn có thể xem danh mục sản phẩm, tìm theo tên hoặc lọc theo danh mục và khoảng giá. Trên trang sản phẩm, nút dấu cộng sẽ thêm nhanh số lượng tối thiểu vào giỏ.', actions: [
                { label: 'Khám phá sản phẩm', path: '/products' }
            ] };
        }
        if (/nhan vien|nguoi that|tu van|gap ho tro|lien he|khong giai quyet|ho tro vien/.test(text)) {
            return { text: 'Để nhân viên tiếp nhận và phản hồi trong tài khoản, hãy đăng nhập rồi tạo yêu cầu tại trang Hỗ trợ. Ghi rõ vấn đề và mã đơn nếu có để đội ngũ kiểm tra nhanh hơn.', actions: [
                { label: 'Mở trang hỗ trợ', path: '/support' }, { label: 'Đăng nhập', path: '/login' }
            ] };
        }
        return { text: 'Mình chưa xác định chính xác câu hỏi này. Bạn thử chọn một chủ đề bên dưới hoặc mở trang Hỗ trợ để gửi yêu cầu chi tiết; trợ lý tự động không thay thế nhân viên chăm sóc khách hàng.', actions: [
            { label: 'Trang hỗ trợ', path: '/support' }, { label: 'Câu hỏi về sản phẩm', path: '/products' }
        ] };
    }

    function ask(question) {
        const value = String(question || '').trim();
        if (!value) return;
        addMessage('user', value);
        const answer = getAnswer(value);
        window.setTimeout(function () { addMessage('bot', answer.text, answer.actions); }, 180);
    }

    function openPanel() {
        panel.hidden = false;
        launcher.setAttribute('aria-expanded', 'true');
        input.focus();
    }

    function closePanel() {
        panel.hidden = true;
        launcher.setAttribute('aria-expanded', 'false');
        launcher.focus();
    }

    launcher.addEventListener('click', function () {
        if (panel.hidden) openPanel();
        else closePanel();
    });
    if (closeButton) closeButton.addEventListener('click', closePanel);
    form.addEventListener('submit', function (event) {
        event.preventDefault();
        const question = input.value;
        input.value = '';
        ask(question);
    });
    if (quickBar) {
        quickBar.querySelectorAll('[data-chat-question]').forEach(function (button) {
            button.addEventListener('click', function () { ask(button.dataset.chatQuestion); });
        });
    }
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !panel.hidden) closePanel();
    });
})();
