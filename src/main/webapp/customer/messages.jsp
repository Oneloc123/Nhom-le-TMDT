<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tin nhắn xưởng | PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261126-customer-sync-r1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account-modals.css?v=20261126-customer-sync-r1">
    <style>
        .uv2-message-layout {
            display: grid;
            grid-template-columns: minmax(0, 1fr);
            gap: 18px;
            align-items: start;
        }
        @media (max-width: 960px) {
            .uv2-message-layout {
                grid-template-columns: 1fr;
            }
        }
        .uv2-message-threads {
            padding: 20px;
        }
        .uv2-message-thread-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
            max-height: none;
            overflow: visible;
            padding-right: 0;
        }
        .uv2-message-thread {
            display: flex;
            align-items: flex-start;
            gap: 12px;
            padding: 12px 14px;
            border-radius: 12px;
            border: 1px solid #e2e8f0;
            background: #ffffff;
            text-decoration: none;
            color: inherit;
            transition: all 0.18s ease;
            position: relative;
        }
        .uv2-message-thread:hover {
            background: #f8fafc;
            border-color: #cbd5e1;
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
        }
        .uv2-message-thread.is-selected {
            background: #eff6ff;
            border-color: #3b82f6;
            box-shadow: 0 4px 14px rgba(59, 130, 246, 0.12);
        }
        .uv2-message-thread-icon {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            border-radius: 10px;
            background: #f1f5f9;
            color: #475569;
            font-size: 16px;
            flex-shrink: 0;
            margin-top: 2px;
            transition: all 0.18s ease;
        }
        .uv2-message-thread.is-selected .uv2-message-thread-icon {
            background: #2563eb;
            color: #ffffff;
        }
        .uv2-message-thread-copy {
            display: flex;
            flex-direction: column;
            gap: 3px;
            min-width: 0;
            flex: 1;
        }
        .uv2-message-thread-copy b {
            font-size: 13.5px;
            font-weight: 700;
            color: #0f172a;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            display: block;
        }
        .uv2-message-thread-copy small {
            font-size: 11.5px;
            color: #64748b;
            display: flex;
            align-items: center;
            gap: 5px;
            flex-wrap: wrap;
        }
        .uv2-message-thread-copy span {
            font-size: 12.5px;
            color: #475569;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            display: block;
            margin-top: 1px;
            line-height: 1.4;
        }
        .uv2-message-thread-copy small.uv2-thread-time {
            font-size: 10.5px;
            color: #94a3b8;
            margin-top: 3px;
        }
        .uv2-thread-badge-escalation {
            display: inline-flex;
            align-items: center;
            gap: 3px;
            padding: 2px 6px;
            border-radius: 5px;
            background: #fee2e2;
            border: 1px solid #fecaca;
            color: #dc2626;
            font-size: 10px;
            font-weight: 700;
        }
        .uv2-thread-unread-pill {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 20px;
            height: 20px;
            padding: 0 6px;
            border-radius: 99px;
            background: #ef4444;
            color: #ffffff;
            font-size: 11px;
            font-weight: 700;
            flex-shrink: 0;
            align-self: center;
        }
        .uv2-message-conversation {
            padding: 22px;
            min-width: 0;
        }
        .uv2-message-placeholder {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 280px;
            text-align: center;
            color: #64748b;
            padding: 30px;
        }
        .uv2-escalation-banner {
            margin-bottom: 16px;
            padding: 14px 16px;
            border-radius: 12px;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            color: #1e40af;
        }
        .uv2-escalate-details {
            margin-bottom: 16px;
        }
        .uv2-escalate-summary {
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 7px 14px;
            border-radius: 8px;
            border: 1px solid #fed7aa;
            background: #fff7ed;
            color: #c2410c;
            font-size: 12px;
            font-weight: 700;
            transition: all 0.15s ease;
            user-select: none;
        }
        .uv2-escalate-summary:hover {
            background: #ffedd5;
            border-color: #fb923c;
        }
        .uv2-escalate-form {
            margin-top: 10px;
            padding: 14px 16px;
            border: 1px solid #fed7aa;
            border-radius: 12px;
            background: #fffaf0;
        }
        .uv2-escalate-form label {
            display: block;
            margin-bottom: 6px;
            font-weight: 700;
            font-size: 12px;
            color: #9a3412;
        }
        .uv2-escalate-form textarea {
            width: 100%;
            box-sizing: border-box;
            border: 1px solid #fdba74;
            border-radius: 8px;
            padding: 10px 12px;
            font: inherit;
            font-size: 12.5px;
            margin-bottom: 10px;
            outline: none;
            background: #ffffff;
        }
        .uv2-escalate-form textarea:focus {
            border-color: #ea580c;
            box-shadow: 0 0 0 3px rgba(234, 88, 12, 0.12);
        }
        .uv2-message-chat {
            display: flex;
            flex-direction: column;
            gap: 10px;
            max-height: 460px;
            min-height: 220px;
            overflow-y: auto;
            padding: 14px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            margin-bottom: 16px;
        }
        .uv2-message-bubble {
            max-width: 82%;
            padding: 10px 14px;
            border-radius: 12px;
            font-size: 13px;
            line-height: 1.5;
            word-break: break-word;
            border: 1px solid #e2e8f0;
            background: #ffffff;
            align-self: flex-start;
            box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
        }
        .uv2-message-bubble.mine {
            align-self: flex-end;
            background: #2563eb;
            border-color: #1d4ed8;
            color: #ffffff;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.18);
        }
        .uv2-message-bubble.mine .uv2-message-bubble-head b {
            color: #ffffff;
        }
        .uv2-message-bubble.mine .uv2-message-bubble-head small {
            color: rgba(255, 255, 255, 0.75);
        }
        .uv2-message-bubble.mine p {
            color: #ffffff;
        }
        .uv2-message-bubble.is-admin {
            align-self: center;
            width: 100%;
            max-width: 96%;
            background: #eff6ff !important;
            border: 1.5px solid #93c5fd !important;
            color: #1e3a8a;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
        }
        .uv2-message-bubble-head {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            margin-bottom: 5px;
            font-size: 11.5px;
        }
        .uv2-message-bubble-head b {
            color: #0f172a;
            font-weight: 700;
        }
        .uv2-message-bubble-head small {
            color: #94a3b8;
            font-size: 10.5px;
        }
        .uv2-message-bubble p {
            margin: 0;
            color: #334155;
            white-space: pre-wrap;
        }
        .uv2-message-form label {
            display: flex;
            flex-direction: column;
            gap: 6px;
            font-size: 12.5px;
            font-weight: 700;
            color: #1e293b;
        }
        .uv2-message-form textarea {
            width: 100%;
            box-sizing: border-box;
            border: 1px solid #cbd5e1;
            border-radius: 10px;
            padding: 10px 12px;
            font: inherit;
            font-size: 13px;
            min-height: 76px;
            outline: none;
            transition: all 0.15s ease;
            background: #ffffff;
        }
        .uv2-message-form textarea:focus {
            border-color: #2563eb;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }
        .uv2-message-form-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            margin-top: 8px;
        }
        .uv2-message-form-footer small {
            color: #64748b;
            font-size: 11.5px;
        }
    </style>
</head>
<body>
<%@ include file="header.jsp" %>
<main class="uv2-page uv2-messages-page">
    <div class="uv2-breadcrumb"><a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span><a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span><span>Tin nhắn xưởng</span></div>
    <div class="uv2-page-heading">
        <div><p class="uv2-eyebrow">TRAO ĐỔI ĐƠN HÀNG</p><h1>Tin nhắn xưởng</h1><p>Trao đổi trực tiếp với xưởng gắn với từng đơn hàng của bạn.</p></div>
    </div>
    <c:if test="${not empty error}"><div class="uv2-alert is-error" role="alert"><c:out value="${error}"/></div></c:if>
    <c:if test="${param.sent eq '1'}"><div class="uv2-alert is-success" role="status">Tin nhắn đã được gửi.</div></c:if>
    <c:if test="${param.escalated eq '1'}"><div class="uv2-alert is-success" role="status">Đã gửi yêu cầu Admin can thiệp. Ban Quản Trị PrintKraft sẽ tham gia hội thoại để hỗ trợ giải quyết.</div></c:if>
    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>
        <div class="uv2-content">
            <div class="uv2-message-layout">
                <section class="uv2-panel uv2-message-threads">
                    <div class="uv2-panel-head"><div><p class="uv2-eyebrow">HỘI THOẠI</p><h2>Đơn hàng và xưởng</h2><p class="uv2-muted">Danh sách hiển thị toàn chiều rộng. Chọn một hội thoại để xem nội dung trong cửa sổ riêng.</p></div></div>
                    <c:choose>
                        <c:when test="${empty threads}"><div class="uv2-empty"><div class="uv2-empty-icon" aria-hidden="true">✉</div><b>Chưa có hội thoại</b><p>Tin nhắn sẽ xuất hiện tại đây khi đơn hàng của bạn có xưởng phụ trách.</p><a class="uv2-button is-light" href="${pageContext.request.contextPath}/purchase-history">Xem đơn hàng</a></div></c:when>
                        <c:otherwise>
                            <div class="uv2-message-thread-list">
                                <c:forEach var="t" items="${threads}">
                                    <a class="uv2-message-thread ${threadOrderId eq t.orderId and threadShopId eq t.shopId ? 'is-selected' : ''}" href="${pageContext.request.contextPath}/messages?orderId=${t.orderId}&amp;shopId=${t.shopId}">
                                        <span class="uv2-message-thread-icon" aria-hidden="true">✉</span>
                                        <span class="uv2-message-thread-copy">
                                            <b><c:out value="${t.shopName}"/></b>
                                            <small>Đơn <c:out value="${t.orderCode}"/><c:if test="${not empty t.escalationStatus}"> · <span class="uv2-thread-badge-escalation">⚖ Admin can thiệp</span></c:if></small>
                                            <span><c:choose><c:when test="${not empty t.lastMessage}"><c:out value="${t.lastMessage}"/></c:when><c:otherwise>Chưa có tin nhắn. Bắt đầu trao đổi với xưởng.</c:otherwise></c:choose></span>
                                            <small class="uv2-thread-time"><c:out value="${t.lastMessageAt}"/></small>
                                        </span>
                                        <c:if test="${t.unread gt 0}"><span class="uv2-thread-unread-pill"><c:out value="${t.unread}"/></span></c:if>
                                        <span class="uv2-thread-open">Xem hội thoại <span aria-hidden="true">↗</span></span>
                                    </a>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
                <dialog id="messageDetailModal" class="account-modal account-modal--wide uv2-message-detail-modal" data-account-modal data-modal-auto-open="${not empty threadOrderId ? 'true' : 'false'}" aria-labelledby="messageDetailHeading">
                    <div class="account-modal-panel">
                        <div class="account-modal-header">
                            <div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">✉</span><div><h2 id="messageDetailHeading">Chi tiết hội thoại<c:if test="${not empty threadOrderId}"> · Đơn hàng #<c:out value="${threadOrderId}"/></c:if></h2><p>Xem lịch sử trao đổi và gửi phản hồi cho xưởng trong cửa sổ này.</p></div></div>
                            <button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button>
                        </div>
                        <div class="account-modal-scroll">
                            <section class="uv2-panel uv2-message-conversation">

                    <c:choose>
                        <c:when test="${empty threadOrderId}"><div class="uv2-empty uv2-message-placeholder"><b>Chọn hội thoại để bắt đầu</b><p>Chọn một đơn hàng ở danh sách bên trái để xem tin nhắn và phản hồi xưởng.</p></div></c:when>
                        <c:otherwise>
                            <c:choose>
                                <c:when test="${not empty activeEscalation}">
                                    <div class="uv2-escalation-banner">
                                        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:4px;">
                                            <strong>⚖ Yêu cầu can thiệp Admin · <c:out value="${activeEscalation.status}"/></strong>
                                            <small><c:out value="${activeEscalation.createdAt}"/></small>
                                        </div>
                                        <p style="margin:0;font-size:12px;line-height:1.5;">
                                            <c:choose>
                                                <c:when test="${activeEscalation.status eq 'IN_PROGRESS'}">
                                                    Admin <b><c:out value="${activeEscalation.adminName}"/></b> đã tham gia hội thoại 3 bên để hỗ trợ giải quyết vấn đề.
                                                </c:when>
                                                <c:otherwise>
                                                    Yêu cầu đã được ghi nhận. Ban Quản Trị đang tiếp nhận và sẽ sớm phản hồi tại đây.
                                                </c:otherwise>
                                            </c:choose>
                                        </p>
                                        <c:if test="${not empty activeEscalation.reason}">
                                            <small style="display:block;margin-top:4px;color:#2563eb;">Lý do: <c:out value="${activeEscalation.reason}"/></small>
                                        </c:if>
                                        <c:if test="${not empty activeEscalation.verdict}">
                                            <div style="margin-top:8px;padding:8px 12px;border-radius:8px;background:#fff;border:1px solid #93c5fd;color:#1e3a8a;">
                                                <strong>Phán quyết của Admin:</strong> <c:out value="${activeEscalation.verdict}"/>
                                            </div>
                                        </c:if>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    
<div class="account-modal-actions"><button type="button" class="account-modal-button is-light" data-modal-open="#messageEscalateModal">⚖ Yêu cầu Admin can thiệp</button></div>
                                </c:otherwise>
                            </c:choose>
                            <div class="account-ticket-chat uv2-message-chat" aria-live="polite">
                                <c:forEach var="m" items="${threadMessages}">
                                    <article class="account-ticket-message uv2-message-bubble <c:if test='${m.mine}'>mine</c:if> <c:if test='${m.senderRole eq \"ADMIN\"}'>is-admin</c:if>">
                                        <div class="uv2-message-bubble-head">
                                            <b>
                                                <c:choose>
                                                    <c:when test="${m.senderRole eq 'ADMIN'}">
                                                        <span style="display:inline-flex;align-items:center;gap:4px;background:#2563eb;color:#fff;padding:2px 8px;border-radius:6px;font-size:10.5px;font-weight:700;">🛡️ Admin (<c:out value="${m.sender}"/>)</span>
                                                    </c:when>
                                                    <c:when test="${m.mine}">
                                                        <c:out value="${m.sender}"/> (Bạn)
                                                    </c:when>
                                                    <c:when test="${m.senderRole eq 'SHOP'}">
                                                        🏬 <c:out value="${m.sender}"/>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <c:out value="${m.sender}"/>
                                                    </c:otherwise>
                                                </c:choose>
                                            </b>
                                            <small><c:out value="${m.createdAt}"/></small>
                                        </div>
                                        <p><c:out value="${m.body}"/></p>
                                    </article>
                                </c:forEach>
                                <c:if test="${empty threadMessages}"><p class="uv2-muted">Chưa có tin nhắn. Bạn có thể gửi lời nhắn đầu tiên cho xưởng.</p></c:if>
                            </div>
                            <form id="messageComposeForm" method="post" action="${pageContext.request.contextPath}/messages" class="uv2-form uv2-message-form uv2-message-composer">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="orderId" value="${threadOrderId}">
                <input type="hidden" name="shopId" value="${threadShopId}">
                <label class="span-full" for="messageBody">Nội dung tin nhắn<textarea id="messageBody" name="message" rows="4" maxlength="2000" required placeholder="Nhập tin nhắn của bạn..."><c:out value="${param.message}"/></textarea></label>
                <div class="uv2-message-form-footer">
                    <small>Tối đa 2.000 ký tự</small>
                    <button type="submit" class="account-modal-button is-primary">Gửi tin nhắn <span aria-hidden="true">➤</span></button>
                </div>
            </form>


                        </c:otherwise>
                    </c:choose>
                </section>
                        </div>
                        <div class="account-modal-footer"><span class="account-modal-footer-note">Lịch sử trao đổi được lưu trong tài khoản.</span><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-primary" data-modal-close>Đóng hội thoại</button></div></div>
                    </div>
                </dialog>
<dialog id="messageEscalateModal" class="account-modal" data-account-modal data-modal-auto-open="${not empty error and param.action eq 'escalate' ? 'true' : 'false'}" aria-labelledby="messageEscalateHeading">
    <div class="account-modal-panel">
        <div class="account-modal-header"><div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">⚖</span><div><h2 id="messageEscalateHeading">Yêu cầu Admin can thiệp</h2><p>Gửi yêu cầu để Ban Quản Trị tham gia hội thoại của đơn hàng này.</p></div></div><button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button></div>
        <div class="account-modal-scroll">
            <form id="messageEscalateForm" method="post" action="${pageContext.request.contextPath}/messages" class="account-modal-form">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="escalate">
                <input type="hidden" name="orderId" value="${threadOrderId}">
                <input type="hidden" name="shopId" value="${threadShopId}">
                <label class="span-full">Lý do yêu cầu Admin vào cuộc<textarea name="reason" rows="4" maxlength="2000" required placeholder="Mô tả vấn đề, thời gian phát sinh, trao đổi đã có và kết quả mong muốn..."></textarea></label>
                <p class="account-modal-form-note span-full">Admin có thể tham gia cùng khách hàng và xưởng trong hội thoại này.</p>
            </form>
        </div>
        <div class="account-modal-footer"><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-light" data-modal-close>Hủy</button><button type="submit" form="messageEscalateForm" class="account-modal-button is-primary">Gửi yêu cầu can thiệp</button></div></div>
    </div>
</dialog>
</div>
        </div>
    </div>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/account-modals.js?v=20261125-modals"></script>
<script>
(() => {
    const chat = document.querySelector('.uv2-message-chat');
    if (chat) chat.scrollTop = chat.scrollHeight;
})();
</script>
</body>
</html>