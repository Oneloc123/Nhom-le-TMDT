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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104-account-layout">
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
    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>
        <div class="uv2-content">
            <div class="uv2-message-layout">
                <section class="uv2-panel uv2-message-threads">
                    <div class="uv2-panel-head"><div><p class="uv2-eyebrow">HỘI THOẠI</p><h2>Đơn hàng và xưởng</h2></div></div>
                    <c:choose>
                        <c:when test="${empty threads}"><div class="uv2-empty"><div class="uv2-empty-icon" aria-hidden="true">✉</div><b>Chưa có hội thoại</b><p>Tin nhắn sẽ xuất hiện tại đây khi đơn hàng của bạn có xưởng phụ trách.</p><a class="uv2-button is-light" href="${pageContext.request.contextPath}/purchase-history">Xem đơn hàng</a></div></c:when>
                        <c:otherwise>
                            <div class="uv2-message-thread-list">
                                <c:forEach var="t" items="${threads}">
                                    <a class="uv2-message-thread ${threadOrderId eq t.orderId and threadShopId eq t.shopId ? 'is-selected' : ''}" href="${pageContext.request.contextPath}/messages?orderId=${t.orderId}&amp;shopId=${t.shopId}">
                                        <span class="uv2-message-thread-icon" aria-hidden="true">✉</span>
                                        <span class="uv2-message-thread-copy"><b><c:out value="${t.shopName}"/></b><small>Đơn <c:out value="${t.orderCode}"/></small><span><c:choose><c:when test="${not empty t.lastMessage}"><c:out value="${t.lastMessage}"/></c:when><c:otherwise>Chưa có tin nhắn. Bắt đầu trao đổi với xưởng.</c:otherwise></c:choose></span><small><c:out value="${t.lastMessageAt}"/></small></span>
                                        <c:if test="${t.unread gt 0}"><span class="uv2-nav-count"><c:out value="${t.unread}"/></span></c:if>
                                    </a>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
                <section class="uv2-panel uv2-message-conversation">
                    <div class="uv2-panel-head"><div><p class="uv2-eyebrow">CHI TIẾT HỘI THOẠI</p><h2><c:choose><c:when test="${not empty threadOrderId}">Đơn hàng #<c:out value="${threadOrderId}"/></c:when><c:otherwise>Chọn một hội thoại</c:otherwise></c:choose></h2></div></div>
                    <c:choose>
                        <c:when test="${empty threadOrderId}"><div class="uv2-empty uv2-message-placeholder"><b>Chọn hội thoại để bắt đầu</b><p>Chọn một đơn hàng ở danh sách bên trái để xem tin nhắn và phản hồi xưởng.</p></div></c:when>
                        <c:otherwise>
                            <div class="account-ticket-chat uv2-message-chat" aria-live="polite">
                                <c:forEach var="m" items="${threadMessages}">
                                    <article class="account-ticket-message uv2-message-bubble <c:if test='${m.mine}'>mine</c:if>">
                                        <div class="uv2-message-bubble-head"><b><c:out value="${m.sender}"/><c:if test="${m.mine}"> (Bạn)</c:if></b><small><c:out value="${m.createdAt}"/></small></div>
                                        <p><c:out value="${m.body}"/></p>
                                    </article>
                                </c:forEach>
                                <c:if test="${empty threadMessages}"><p class="uv2-muted">Chưa có tin nhắn. Bạn có thể gửi lời nhắn đầu tiên cho xưởng.</p></c:if>
                            </div>
                            <form method="post" action="${pageContext.request.contextPath}/messages" class="uv2-form uv2-message-form">
                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                <input type="hidden" name="orderId" value="${threadOrderId}">
                                <input type="hidden" name="shopId" value="${threadShopId}">
                                <label for="messageBody">Tin nhắn<textarea id="messageBody" name="message" rows="3" maxlength="2000" required placeholder="Hỏi tiến độ hoặc trao đổi về đơn hàng..."></textarea></label>
                                <div class="uv2-message-form-footer"><small>Không gửi mật khẩu, OTP hoặc thông tin thẻ thanh toán.</small><button class="uv2-button is-primary" type="submit">Gửi tin nhắn</button></div>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </div>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>