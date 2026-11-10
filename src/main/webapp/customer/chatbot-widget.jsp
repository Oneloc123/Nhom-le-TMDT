<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/customer-chatbot.css?v=20261110-1">
<div class="pk-chat-widget" id="pkChatWidget" data-context-path="${pageContext.request.contextPath}">
    <section class="pk-chat-panel" id="pkChatPanel" hidden aria-label="Chatbot hỗ trợ khách hàng">
        <div class="pk-chat-header">
            <div class="pk-chat-avatar" aria-hidden="true">✦</div>
            <div class="pk-chat-title">
                <strong>Trợ lý PrintKraft</strong>
                <small>Hỏi đáp nhanh về mua hàng và đơn in</small>
            </div>
            <button class="pk-chat-close" id="pkChatClose" type="button" aria-label="Đóng chatbot">×</button>
        </div>
        <div class="pk-chat-messages" id="pkChatMessages" aria-live="polite" aria-label="Nội dung trò chuyện">
            <div class="pk-chat-line">
                <div class="pk-chat-bubble">Xin chào! Mình có thể giúp bạn tìm sản phẩm, theo dõi đơn hàng, hỏi về báo giá hoặc hướng dẫn sử dụng giỏ hàng. Bạn cần hỗ trợ điều gì?</div>
            </div>
            <div class="pk-chat-quick" id="pkChatQuick">
                <button type="button" data-chat-question="Tôi muốn theo dõi đơn hàng">Theo dõi đơn</button>
                <button type="button" data-chat-question="Tôi muốn hỏi báo giá">Báo giá</button>
                <button type="button" data-chat-question="Tôi cần hỗ trợ thiết kế 3D">Thiết kế 3D</button>
                <button type="button" data-chat-question="Tôi muốn gặp nhân viên hỗ trợ">Gặp hỗ trợ</button>
            </div>
        </div>
        <form class="pk-chat-form" id="pkChatForm" autocomplete="off">
            <label class="visually-hidden" for="pkChatInput">Nhập câu hỏi</label>
            <input class="pk-chat-input" id="pkChatInput" type="text" maxlength="500" placeholder="Nhập câu hỏi của bạn…" required>
            <button class="pk-chat-send" type="submit" aria-label="Gửi câu hỏi">➤</button>
        </form>
        <div class="pk-chat-disclaimer">Trợ lý tự động · Có thể chuyển sang trang hỗ trợ để nhân viên tiếp nhận</div>
    </section>
    <button class="pk-chat-launcher" id="pkChatLauncher" type="button" aria-controls="pkChatPanel" aria-expanded="false">
        <span class="pk-chat-launcher-icon" aria-hidden="true">✦</span><span>Hỗ trợ khách hàng</span>
    </button>
</div>
<script defer src="${pageContext.request.contextPath}/js/customer-chatbot.js?v=20261110-1"></script>
