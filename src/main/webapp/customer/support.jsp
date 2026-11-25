<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hỗ trợ và khiếu nại | PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104-account-layout">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account-modals.css?v=20261125-modals">
    <style>
        .uv2-support-grid {
            display: grid;
            grid-template-columns: minmax(320px, 1fr) minmax(340px, 1.2fr);
            gap: 20px;
            align-items: start;
        }
        @media (max-width: 960px) {
            .uv2-support-grid {
                grid-template-columns: 1fr;
            }
        }
        .uv2-ticket-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
            max-height: 580px;
            overflow-y: auto;
            padding-right: 4px;
        }
        .uv2-ticket-item {
            padding: 14px 16px;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            background: #ffffff;
            transition: all 0.18s ease;
        }
        .uv2-ticket-item:hover {
            background: #f8fafc;
            border-color: #cbd5e1;
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
        }
        .uv2-ticket-item.is-selected {
            background: #eff6ff;
            border-color: #3b82f6;
            box-shadow: 0 4px 14px rgba(59, 130, 246, 0.12);
        }
        .uv2-ticket-item-top {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;
        }
        .uv2-ticket-code {
            font-size: 11px;
            font-weight: 800;
            color: #2563eb;
            letter-spacing: 0.5px;
        }
        .uv2-ticket-item h3 {
            margin: 0 0 6px;
            font-size: 13.5px;
            font-weight: 700;
            color: #0f172a;
            line-height: 1.4;
            overflow-wrap: anywhere;
        }
        .uv2-ticket-meta {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 6px;
            color: #64748b;
            font-size: 11.5px;
            margin: 0 0 10px;
        }
        .uv2-ticket-meta time {
            color: #94a3b8;
            font-size: 11px;
        }
        .uv2-ticket-attachments {
            margin-bottom: 14px;
            padding: 12px 14px;
            border: 1px solid #e2e8f0;
            border-radius: 10px;
            background: #f8fafc;
            font-size: 12px;
        }
        .uv2-ticket-attachments ul {
            margin: 6px 0 0;
            padding-left: 20px;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }
        .uv2-ticket-attachments a {
            color: #2563eb;
            font-weight: 600;
            text-decoration: underline;
        }
        .uv2-ticket-chat {
            display: flex;
            flex-direction: column;
            gap: 10px;
            max-height: 440px;
            overflow-y: auto;
            padding: 14px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            margin: 14px 0;
        }
        .uv2-ticket-actions {
            display: flex;
            align-items: flex-end;
            flex-wrap: wrap;
            gap: 14px;
            margin-top: 14px;
            padding-top: 14px;
            border-top: 1px solid #edf2f7;
        }
        .uv2-rate-form {
            display: flex;
            align-items: flex-end;
            gap: 10px;
            flex-wrap: wrap;
        }
        .uv2-rate-form label {
            display: flex;
            flex-direction: column;
            gap: 4px;
            font-size: 12px;
            font-weight: 700;
            color: #334155;
        }
        .uv2-rate-form select {
            height: 38px;
            padding: 0 10px;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            font: inherit;
            font-size: 12.5px;
        }
    </style>
</head>
<body>
<jsp:include page="/customer/header.jsp"/>
<main class="uv2-page">
    <div class="uv2-breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span>
        <a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span>
        <span>Hỗ trợ và khiếu nại</span>
    </div>
    <div class="uv2-page-heading">
        <div>
            <p class="uv2-eyebrow">CHĂM SÓC KHÁCH HÀNG</p>
            <h1>Hỗ trợ và khiếu nại</h1>
            <p>Tạo ticket để liên hệ đội ngũ PrintKraft; lịch sử trao đổi được lưu trong tài khoản.</p>
        </div>
    </div>
    <c:if test="${not empty error}"><div class="uv2-alert is-error" role="alert"><c:out value="${error}"/></div></c:if>
    <c:if test="${param.created eq '1'}"><div class="uv2-alert is-success" role="status">Ticket đã được tạo. Đội ngũ hỗ trợ sẽ sớm phản hồi.</div></c:if>
    <c:if test="${param.replied eq '1'}"><div class="uv2-alert is-success" role="status">Phản hồi của bạn đã được gửi.</div></c:if>
    <c:if test="${param.reopened eq '1'}"><div class="uv2-alert is-success" role="status">Yêu cầu đã được mở lại.</div></c:if>
    <c:if test="${param.rated eq '1'}"><div class="uv2-alert is-success" role="status">Cảm ơn bạn đã gửi đánh giá.</div></c:if>

    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>
        <div class="uv2-content">
            <c:if test="${not empty ticket}">
                <section class="uv2-panel" id="ticket-detail" style="margin-bottom:20px;">
                    <div class="uv2-panel-head">
                        <div>
                            <p class="uv2-eyebrow"><c:out value="${ticket.code}"/></p>
                            <h2><c:out value="${ticket.subject}"/></h2>
                            <p>Trạng thái: <span class="uv2-status status-<c:out value="${ticket.status}"/>"><c:out value="${ticket.status}"/></span> · Phân loại: <b><c:out value="${ticket.category}"/></b> · Ưu tiên: <b><c:out value="${ticket.priority}"/></b></p>
                        </div>
                        <a class="uv2-button is-light" href="${pageContext.request.contextPath}/support">Đóng chi tiết ✕</a>
                    </div>
                    <c:if test="${not empty ticketAttachments}">
                        <div class="uv2-ticket-attachments">
                            <b>Tệp đính kèm:</b>
                            <ul>
                                <c:forEach var="a" items="${ticketAttachments}">
                                    <li><a href="${pageContext.request.contextPath}/support/attachment?id=${a.id}">📎 <c:out value="${a.fileName}"/></a> (<c:out value="${a.size}"/> byte)</li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>
                    <div class="account-ticket-chat uv2-ticket-chat" aria-live="polite">
                        <c:forEach var="m" items="${ticketMessages}">
                            <article class="account-ticket-message uv2-message-bubble">
                                <div class="uv2-message-bubble-head">
                                    <b><c:out value="${m.sender}"/></b>
                                    <small><c:out value="${m.createdAt}"/></small>
                                </div>
                                <p><c:out value="${m.body}"/></p>
                            </article>
                        </c:forEach>
                    </div>
                    <c:if test="${ticket.status != 'CLOSED'}">
                        
<div class="account-modal-actions"><button type="button" class="account-modal-button is-primary" data-modal-open="#supportReplyModal">＋ Gửi phản hồi</button></div>
<dialog id="supportReplyModal" class="account-modal" data-account-modal data-modal-auto-open="${not empty error and param.action eq 'reply' ? 'true' : 'false'}" aria-labelledby="supportReplyHeading">
    <div class="account-modal-panel">
        <div class="account-modal-header"><div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">↩</span><div><h2 id="supportReplyHeading">Gửi phản hồi bổ sung</h2><p>Trao đổi tiếp về ticket <c:out value="${ticket.code}"/>.</p></div></div><button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button></div>
        <div class="account-modal-scroll">
            <aside class="account-modal-help" id="supportReplyHelp" data-modal-help><div class="account-modal-help-title">Trước khi gửi phản hồi</div><ol><li>Đề cập mã ticket hoặc chi tiết liên quan nếu cần.</li><li>Ghi thêm bằng chứng, thời gian hoặc kết quả xử lý đã thử.</li><li>Không gửi mật khẩu, OTP hoặc thông tin thẻ thanh toán.</li></ol></aside>
            <form id="supportReplyForm" method="post" action="${pageContext.request.contextPath}/support" class="uv2-form account-modal-form">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="reply">
                <input type="hidden" name="ticketId" value="${ticket.id}">
                <label class="span-full">Phản hồi bổ sung<textarea name="message" maxlength="5000" rows="4" required placeholder="Nhập phản hồi cho đội ngũ hỗ trợ..."><c:out value="${param.message}"/></textarea><small class="account-modal-field-hint">Từ 1 đến 5.000 ký tự.</small></label>
            </form>
        </div>
        <div class="account-modal-footer"><button type="button" class="account-modal-help-toggle" data-help-toggle aria-controls="supportReplyHelp" aria-expanded="true">Ẩn hướng dẫn</button><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-light" data-modal-close>Hủy</button><button type="submit" form="supportReplyForm" class="account-modal-button is-primary">Gửi phản hồi</button></div></div>
    </div>
</dialog>
                    </c:if>
                    <c:if test="${ticket.status == 'CLOSED' or ticket.status == 'RESOLVED'}">
                        
<div class="uv2-ticket-actions account-modal-actions">
    <button type="button" class="account-modal-button is-light" data-modal-open="#supportReopenModal">Mở lại ticket</button>
    <button type="button" class="account-modal-button is-light" data-modal-open="#supportRateModal">Đánh giá hỗ trợ</button>
</div>
<dialog id="supportReopenModal" class="account-modal" data-account-modal data-modal-auto-open="${not empty error and param.action eq 'reopen' ? 'true' : 'false'}" aria-labelledby="supportReopenHeading">
    <div class="account-modal-panel">
        <div class="account-modal-header"><div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">↻</span><div><h2 id="supportReopenHeading">Mở lại yêu cầu hỗ trợ?</h2><p>Yêu cầu gửi lại để tiếp tục xử lý.</p></div></div><button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button></div>
        <div class="account-modal-scroll">
            <aside class="account-modal-help" id="supportReopenHelp" data-modal-help><div class="account-modal-help-title">Lưu ý</div><ul><li>Chỉ mở lại nếu vấn đề vẫn chưa được giải quyết hoặc cần bổ sung thông tin.</li><li>Chức năng mở lại áp dụng trong 7 ngày theo quy định hiện tại.</li></ul></aside>
            <p class="account-modal-confirm-copy">Bạn muốn mở lại ticket <b><c:out value="${ticket.code}"/></b>?</p>
            <form id="supportReopenForm" method="post" action="${pageContext.request.contextPath}/support"><input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>"><input type="hidden" name="action" value="reopen"><input type="hidden" name="ticketId" value="${ticket.id}"></form>
        </div>
        <div class="account-modal-footer"><button type="button" class="account-modal-help-toggle" data-help-toggle aria-controls="supportReopenHelp" aria-expanded="true">Ẩn hướng dẫn</button><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-light" data-modal-close>Hủy</button><button type="submit" form="supportReopenForm" class="account-modal-button is-primary">Xác nhận mở lại</button></div></div>
    </div>
</dialog>
<dialog id="supportRateModal" class="account-modal" data-account-modal data-modal-auto-open="${not empty error and param.action eq 'rate' ? 'true' : 'false'}" aria-labelledby="supportRateHeading">
    <div class="account-modal-panel">
        <div class="account-modal-header"><div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">★</span><div><h2 id="supportRateHeading">Đánh giá hỗ trợ</h2><p>Đánh giá trải nghiệm xử lý ticket của bạn.</p></div></div><button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button></div>
        <div class="account-modal-scroll">
            <aside class="account-modal-help" id="supportRateHelp" data-modal-help><div class="account-modal-help-title">Chọn mức độ phù hợp</div><ul><li>5 sao: Rất hài lòng.</li><li>3 sao: Bình thường.</li><li>1 sao: Không hài lòng.</li></ul><p>Đánh giá giúp PrintKraft cải thiện chất lượng hỗ trợ.</p></aside>
            <form id="supportRateForm" method="post" action="${pageContext.request.contextPath}/support" class="account-modal-form"><input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>"><input type="hidden" name="action" value="rate"><input type="hidden" name="ticketId" value="${ticket.id}"><label class="span-full">Mức đánh giá<select name="rating"><option value="5">5 ★ - Rất hài lòng</option><option value="4">4 ★ - Hài lòng</option><option value="3">3 ★ - Bình thường</option><option value="2">2 ★ - Chưa hài lòng</option><option value="1">1 ★ - Không hài lòng</option></select></label></form>
        </div>
        <div class="account-modal-footer"><button type="button" class="account-modal-help-toggle" data-help-toggle aria-controls="supportRateHelp" aria-expanded="true">Ẩn hướng dẫn</button><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-light" data-modal-close>Hủy</button><button type="submit" form="supportRateForm" class="account-modal-button is-primary">Gửi đánh giá</button></div></div>
    </div>
</dialog>

                    </c:if>
                </section>
            </c:if>

            <div class="uv2-support-grid">
                <section class="uv2-panel">
                    <div class="uv2-panel-head">
                        <div>
                            <p class="uv2-eyebrow">TICKET CỦA BẠN</p>
                            <h2>Danh sách yêu cầu</h2>
                        </div>
                    </div>
                    <c:choose>
                        <c:when test="${empty tickets}">
                            <div class="uv2-empty">
                                <div class="uv2-empty-icon" aria-hidden="true">🎫</div>
                                <b>Chưa có yêu cầu hỗ trợ</b>
                                <p>Bạn chưa gửi yêu cầu hoặc khiếu nại nào.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="uv2-ticket-list">
                                <c:forEach var="t" items="${tickets}">
                                    <article class="uv2-ticket-item <c:if test="${ticket.id eq t.id}">is-selected</c:if>">
                                        <div class="uv2-ticket-item-top">
                                            <span class="uv2-ticket-code"><c:out value="${t.code}"/></span>
                                            <span class="uv2-status"><c:out value="${t.status}"/></span>
                                        </div>
                                        <h3><c:out value="${t.subject}"/></h3>
                                        <div class="uv2-ticket-meta">
                                            <span><c:out value="${t.category}"/></span> ·
                                            <span>Ưu tiên: <c:out value="${t.priority}"/></span> ·
                                            <time><c:out value="${t.createdAt}"/></time>
                                        </div>
                                        <a class="uv2-button is-light" href="${pageContext.request.contextPath}/support?id=${t.id}">Xem hội thoại →</a>
                                    </article>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                
<section class="uv2-panel account-modal-launch">
    <div><p class="uv2-eyebrow">YÊU CẦU MỚI</p><strong>Vấn đề cần hỗ trợ?</strong><p>Tạo ticket để theo dõi trao đổi, phản hồi và kết quả xử lý ở một nơi.</p></div>
    <button type="button" class="account-modal-button is-primary" data-modal-open="#supportCreateModal">＋ Tạo ticket</button>
</section>
<dialog id="supportCreateModal" class="account-modal" data-account-modal data-modal-auto-open="${not empty error and param.action eq 'create' ? 'true' : 'false'}" aria-labelledby="supportCreateHeading">
    <div class="account-modal-panel">
        <div class="account-modal-header"><div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">🎫</span><div><h2 id="supportCreateHeading">Tạo yêu cầu hỗ trợ / khiếu nại</h2><p>Điền thông tin rõ ràng để yêu cầu được phân loại chính xác.</p></div></div><button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button></div>
        <div class="account-modal-scroll">
            <aside class="account-modal-help" id="supportCreateHelp" data-modal-help><div class="account-modal-help-title">Cách gửi yêu cầu hiệu quả</div><ol><li>Chọn đúng loại vấn đề: hỗ trợ, khiếu nại, thanh toán, chất lượng hoặc vận chuyển.</li><li>Nếu liên quan đến đơn hàng, chọn đơn đó để đội ngũ hỗ trợ tra cứu nhanh hơn.</li><li>Đặt tiêu đề ngắn gọn và mô tả chi tiết chuyện đã xảy ra cùng kết quả bạn mong muốn.</li><li>Đính kèm ảnh/PDF khi cần; kích thước tối đa 8 MB.</li></ol><p>Không gửi mật khẩu, mã OTP hoặc thông tin thẻ thanh toán.</p></aside>
            ${createForm}
        </div>
        <div class="account-modal-footer"><button type="button" class="account-modal-help-toggle" data-help-toggle aria-controls="supportCreateHelp" aria-expanded="true">Ẩn hướng dẫn</button><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-light" data-modal-close>Hủy</button><button type="submit" form="supportCreateForm" class="account-modal-button is-primary">Gửi yêu cầu hỗ trợ</button></div></div>
    </div>
</dialog>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/customer/footer.jsp"/>
<script defer src="${pageContext.request.contextPath}/js/account-modals.js?v=20261125-modals"></script>
<script>
(() => {
    const chat = document.querySelector('.uv2-ticket-chat');
    if (chat) chat.scrollTop = chat.scrollHeight;
})();
</script>
</body>
</html>
