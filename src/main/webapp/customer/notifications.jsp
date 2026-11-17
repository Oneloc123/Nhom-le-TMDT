<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thông báo - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104-account-layout">
    <style>
.notification-filter-bar{display:flex;flex-wrap:wrap;gap:9px;margin:0 0 18px}
.notification-filter{display:inline-flex;align-items:center;gap:8px;padding:9px 13px;border:1px solid #dfe6f1;border-radius:999px;background:#fff;color:#58677f;font-size:12px;font-weight:750;text-decoration:none}
.notification-filter:hover,.notification-filter.is-active{border-color:#9eb2f3;background:#edf2ff;color:#2f50c8}
.notification-filter span{display:inline-grid;min-width:20px;height:20px;place-items:center;padding:0 5px;border-radius:999px;background:#f0f3f8;color:#596982;font-size:10px}
.account-message-row.unread{border-left:3px solid #5273e9}
</style>
</head>
<body class="customer-account-body">
<%@ include file="header.jsp" %>

<main class="uv2-page">
    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>

        <div class="uv2-content">

            <%-- Heading + nút "Đánh dấu tất cả" --%>
            <div class="uv2-page-heading">
                <div>
                    <p class="uv2-eyebrow">TÀI KHOẢN</p>
                    <h1>Thông báo</h1>
                    <p>Bạn có <b><c:out value="${unreadNotifications}"/></b> thông báo chưa đọc.</p>
                </div>
                <form method="post" action="${pageContext.request.contextPath}/notifications">
                    <input type="hidden" name="csrfToken"
                           value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                    <input type="hidden" name="action" value="markAll"><input type="hidden" name="filter" value="<c:out value='${filter}'/>">
                    <button class="uv2-button is-light" type="submit">
                        Đánh dấu tất cả đã đọc
                    </button>
                </form>
            </div>
            <nav class="notification-filter-bar" aria-label="Lọc lịch sử thông báo">
                <a class="notification-filter <c:if test='${filter == "ALL"}'>is-active</c:if>" href="${pageContext.request.contextPath}/notifications?filter=ALL">Tất cả <span><c:out value="${allNotifications}"/></span></a>
                <a class="notification-filter <c:if test='${filter == "UNREAD"}'>is-active</c:if>" href="${pageContext.request.contextPath}/notifications?filter=UNREAD">Chưa đọc <span><c:out value="${unreadNotifications}"/></span></a>
                <a class="notification-filter <c:if test='${filter == "READ"}'>is-active</c:if>" href="${pageContext.request.contextPath}/notifications?filter=READ">Đã đọc <span><c:out value="${readNotifications}"/></span></a>
            </nav>
            <%-- Flash success --%>
            <c:if test="${param.read == '1'}">
                <div class="uv2-alert is-success" role="status">
                    Đã cập nhật trạng thái thông báo.
                </div>
            </c:if>

            <%-- Danh sách thông báo --%>
            <c:choose>
                <c:when test="${empty notifications}">
                    <div class="uv2-panel">
                        <div class="uv2-empty">
                            <b>Chưa có thông báo nào</b>
                            <p>Các cập nhật về đơn hàng, báo giá và tin nhắn sẽ xuất hiện tại đây.</p>
                            <a class="uv2-button is-primary"
                               href="${pageContext.request.contextPath}/products">
                                Khám phá sản phẩm
                            </a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="account-message-stream">
                        <c:forEach var="item" items="${notifications}">
                            <article class="account-message-row <c:if test='${!item.read}'>unread</c:if>">
                                <div class="account-message-meta">
                                    <c:out value="${item.createdAt}"/> ·
                                    <c:choose>
                                        <c:when test="${item.read}">Đã đọc</c:when>
                                        <c:otherwise>Chưa đọc</c:otherwise>
                                    </c:choose>
                                </div>
                                <h2><c:out value="${item.title}"/></h2>
                                <p><c:out value="${item.message}"/></p>
                                <div class="account-shortcuts">
                                    <c:if test="${not empty item.url and fn:startsWith(item.url, '/')}">
                                        <c:url var="noticeUrl" value="${item.url}"/>
                                        <a class="account-outline-button"
                                           href="<c:out value='${noticeUrl}'/>">
                                            Mở nội dung liên quan
                                        </a>
                                    </c:if>
                                    <c:if test="${!item.read}">
                                        <form method="post"
                                              action="${pageContext.request.contextPath}/notifications">
                                            <input type="hidden" name="csrfToken"
                                                   value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="markOne"><input type="hidden" name="filter" value="<c:out value='${filter}'/>">
                                            <input type="hidden" name="id" value="${item.id}">
                                            <button type="submit" class="account-quiet-button">
                                                Đánh dấu đã đọc
                                            </button>
                                        </form>
                                    </c:if>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <%-- Phân trang --%>
            <c:if test="${totalPages gt 1}">
                <div class="uv2-pagination">
                    <c:if test="${page gt 1}">
                        <a class="account-outline-button"
                           href="${pageContext.request.contextPath}/notifications?page=${page - 1}&amp;filter=${filter}">
                            ← Trang trước
                        </a>
                    </c:if>
                    <span>Trang ${page} / ${totalPages}</span>
                    <c:if test="${page lt totalPages}">
                        <a class="account-outline-button"
                           href="${pageContext.request.contextPath}/notifications?page=${page + 1}&amp;filter=${filter}">
                            Trang sau →
                        </a>
                    </c:if>
                </div>
            </c:if>

        </div>
    </div>
</main>
</body>
</html>