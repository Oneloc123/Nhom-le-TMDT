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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261125-notifications">
    <style>
.notification-filter-bar{display:flex;flex-wrap:wrap;gap:9px;margin:0 0 18px}
.notification-filter{display:inline-flex;align-items:center;gap:8px;padding:9px 13px;border:1px solid #dfe6f1;border-radius:999px;background:#fff;color:#58677f;font-size:12px;font-weight:750;text-decoration:none;transition:background .18s,border-color .18s,transform .18s}
.notification-filter:hover,.notification-filter.is-active{border-color:#9eb2f3;background:#edf2ff;color:#2f50c8}
.notification-filter:hover{transform:translateY(-1px)}
.notification-filter span{display:inline-grid;min-width:20px;height:20px;place-items:center;padding:0 5px;border-radius:999px;background:#f0f3f8;color:#596982;font-size:10px}
.account-message-stream{display:grid;gap:13px}
.account-message-row{position:relative;display:grid;grid-template-columns:46px minmax(0,1fr);gap:15px;padding:18px;border:1px solid #e5eaf3;border-radius:17px;background:#fff;box-shadow:0 8px 22px rgba(32,48,85,.035);transition:border-color .18s,box-shadow .18s,transform .18s}
.account-message-row:hover{border-color:#d1daf0;box-shadow:0 12px 28px rgba(32,48,85,.07);transform:translateY(-1px)}
.account-message-row.unread{border-color:#c3d0ff;background:linear-gradient(110deg,#f7f9ff 0%,#fff 68%);box-shadow:inset 3px 0 0 #5273e9,0 8px 22px rgba(32,48,85,.04)}
.account-message-icon{display:grid;place-items:center;width:44px;height:44px;border:1px solid #e2e7fb;border-radius:14px;background:#f1f4ff;color:#4c67d5;font-size:20px}
.account-message-row:not(.unread) .account-message-icon{border-color:#e8edf3;background:#f5f7fa;color:#8792a5}
.account-message-main{min-width:0}
.account-message-meta{display:flex;align-items:center;flex-wrap:wrap;gap:8px;margin:0 0 8px;color:#8a95a8;font-size:11px}
.notification-status{display:inline-flex;align-items:center;gap:5px;padding:4px 8px;border-radius:999px;font-size:10px;font-weight:800;letter-spacing:.01em}
.notification-status.is-unread{background:#e7edff;color:#3859cc}
.notification-status.is-read{background:#eff2f6;color:#79859a}
.account-message-row h2{margin:0;color:#263653;font-size:16px;font-weight:850;line-height:1.45;letter-spacing:-.02em;overflow-wrap:anywhere}
.account-message-row p{margin:8px 0 0;color:#69768d;font-size:12.5px;line-height:1.75;overflow-wrap:anywhere}
.account-shortcuts{display:flex;align-items:center;flex-wrap:wrap;gap:9px;margin-top:14px}
.account-shortcuts form{margin:0}
.account-message-row .account-outline-button,.account-message-row .account-quiet-button{display:inline-flex;align-items:center;justify-content:center;min-height:34px;padding:8px 11px;border:1px solid #dbe3f1;border-radius:10px;background:#fff;color:#4f6285;font:inherit;font-size:11px;font-weight:800;text-decoration:none;cursor:pointer;transition:background .18s,border-color .18s,color .18s}
.account-message-row .account-outline-button:hover,.account-message-row .account-quiet-button:hover{border-color:#aebff6;background:#f0f4ff;color:#3456c5}
.account-message-row .account-quiet-button{border-color:transparent;background:#eef2ff;color:#3d5bc3}
.uv2-pagination{display:flex;align-items:center;justify-content:center;flex-wrap:wrap;gap:12px;margin-top:8px;padding:12px 0;color:#66748c;font-size:12px}
@media(max-width:560px){.account-message-row{grid-template-columns:36px minmax(0,1fr);gap:11px;padding:13px;border-radius:14px}.account-message-icon{width:36px;height:36px;border-radius:11px;font-size:17px}.account-message-row h2{font-size:14px}.account-message-row p{font-size:12px}.account-message-row .account-outline-button,.account-message-row .account-quiet-button{width:100%}.account-shortcuts form{width:100%}}
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
                                <div class="account-message-icon" aria-hidden="true"><c:choose><c:when test="${item.read}">✓</c:when><c:otherwise>🔔</c:otherwise></c:choose></div>
                                <div class="account-message-main">
                                    <div class="account-message-meta">
                                        <span class="notification-status ${item.read ? 'is-read' : 'is-unread'}">
                                            <c:choose><c:when test="${item.read}">✓ Đã đọc</c:when><c:otherwise>● Chưa đọc</c:otherwise></c:choose>
                                        </span>
                                        <span><c:out value="${item.createdAt}"/></span>
                                    </div>
                                    <h2><c:out value="${item.title}"/></h2>
                                    <p><c:out value="${item.message}"/></p>
                                    <div class="account-shortcuts">
                                        <c:if test="${not empty item.url and fn:startsWith(item.url, '/')}">
                                            <c:url var="noticeUrl" value="${item.url}"/>
                                            <a class="account-outline-button" href="<c:out value='${noticeUrl}'/>">
                                                Xem nội dung liên quan <span aria-hidden="true">→</span>
                                            </a>
                                        </c:if>
                                        <c:if test="${!item.read}">
                                            <form method="post" action="${pageContext.request.contextPath}/notifications">
                                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                <input type="hidden" name="action" value="markOne">
                                                <input type="hidden" name="filter" value="<c:out value='${filter}'/>">
                                                <input type="hidden" name="id" value="${item.id}">
                                                <button type="submit" class="account-quiet-button">Đánh dấu đã đọc</button>
                                            </form>
                                        </c:if>
                                    </div>
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