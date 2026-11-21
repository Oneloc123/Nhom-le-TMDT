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
                        <form method="post" action="${pageContext.request.contextPath}/support" class="uv2-form uv2-message-form">
                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                            <input type="hidden" name="action" value="reply">
                            <input type="hidden" name="ticketId" value="${ticket.id}">
                            <label>Phản hồi bổ sung:
                                <textarea name="message" maxlength="5000" rows="3" required placeholder="Nhập phản hồi cho đội ngũ hỗ trợ..."></textarea>
                            </label>
                            <div class="uv2-message-form-footer">
                                <small>Nội dung phản hồi từ 1 đến 5.000 ký tự.</small>
                                <button class="uv2-button is-primary" type="submit">Gửi phản hồi</button>
                            </div>
                        </form>
                    </c:if>
                    <c:if test="${ticket.status == 'CLOSED' or ticket.status == 'RESOLVED'}">
                        <div class="uv2-ticket-actions">
                            <form method="post" action="${pageContext.request.contextPath}/support" style="display:inline;">
                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                <input type="hidden" name="action" value="reopen">
                                <input type="hidden" name="ticketId" value="${ticket.id}">
                                <button class="uv2-button is-light" type="submit">Mở lại ticket (trong 7 ngày)</button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/support" class="uv2-rate-form">
                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                <input type="hidden" name="action" value="rate">
                                <input type="hidden" name="ticketId" value="${ticket.id}">
                                <label>Đánh giá hỗ trợ:
                                    <select name="rating">
                                        <option value="5">5 ★ - Rất hài lòng</option>
                                        <option value="4">4 ★ - Hài lòng</option>
                                        <option value="3">3 ★ - Bình thường</option>
                                        <option value="2">2 ★ - Chưa hài lòng</option>
                                        <option value="1">1 ★ - Không hài lòng</option>
                                    </select>
                                </label>
                                <button class="uv2-button is-light" type="submit">Gửi đánh giá</button>
                            </form>
                        </div>
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

                <section class="uv2-panel">
                    <div class="uv2-panel-head">
                        <div>
                            <p class="uv2-eyebrow">YÊU CẦU MỚI</p>
                            <h2>Tạo ticket hỗ trợ</h2>
                        </div>
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/support" class="uv2-form" enctype="multipart/form-data">
                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                        <input type="hidden" name="action" value="create">
                        <label>Loại yêu cầu
                            <select name="category" required>
                                <option value="SUPPORT">Hỗ trợ</option>
                                <option value="COMPLAINT">Khiếu nại</option>
                                <option value="REFUND">Hoàn tiền</option>
                                <option value="PAYMENT">Thanh toán</option>
                                <option value="QUALITY">Chất lượng sản phẩm</option>
                                <option value="SHIPPING">Vận chuyển</option>
                                <option value="ACCOUNT">Tài khoản</option>
                                <option value="OTHER">Khác</option>
                            </select>
                        </label>
                        <label>Đơn hàng liên quan (không bắt buộc)
                            <select name="orderId">
                                <option value="">Không chọn đơn hàng</option>
                                <c:forEach var="o" items="${orders}">
                                    <option value="${o.id}"><c:out value="${o.code}"/> · <c:out value="${o.status}"/></option>
                                </c:forEach>
                            </select>
                        </label>
                        <label>Tiêu đề
                            <input type="text" name="subject" maxlength="180" required placeholder="Tóm tắt vấn đề cần hỗ trợ">
                        </label>
                        <label>Mức ưu tiên
                            <select name="priority">
                                <option value="LOW">Thấp</option>
                                <option value="NORMAL" selected>Bình thường</option>
                                <option value="HIGH">Cao</option>
                            </select>
                        </label>
                        <label>Nội dung chi tiết
                            <textarea name="message" rows="5" minlength="10" maxlength="5000" required placeholder="Mô tả cụ thể sự cố hoặc yêu cầu cần hỗ trợ..."></textarea>
                        </label>
                        <label>Tệp đính kèm (ảnh JPG/PNG/WebP hoặc PDF, tối đa 8 MB)
                            <input type="file" name="attachment" accept=".jpg,.jpeg,.png,.webp,.pdf,image/jpeg,image/png,image/webp,application/pdf">
                        </label>
                        <button class="uv2-button is-primary" type="submit">Gửi yêu cầu hỗ trợ</button>
                    </form>
                </section>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/customer/footer.jsp"/>
<script>
(() => {
    const chat = document.querySelector('.uv2-ticket-chat');
    if (chat) chat.scrollTop = chat.scrollHeight;
})();
</script>
</body>
</html>
