<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Yêu cầu báo giá - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Doanh nghiệp</div>
            <h1>Yêu cầu báo giá số lượng lớn</h1>
            <p>Gửi yêu cầu báo giá cho đơn hàng doanh nghiệp; xưởng in sẽ phản hồi kèm báo giá.</p>
        </div>
    </div>

    <c:if test="${not empty param.created}">
        <div class="notice-ok">Đã gửi yêu cầu báo giá. Mã yêu cầu: <c:out value="${param.created}"/>.</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="summary-panel">
            <h2>Yêu cầu của tôi</h2>
            <c:choose>
                <c:when test="${empty quotes}">
                    <div class="empty-state">Bạn chưa gửi yêu cầu báo giá nào.</div>
                </c:when>
                <c:otherwise>
                    <div class="cart-list">
                        <c:forEach var="q" items="${quotes}">
                            <div class="cart-line">
                                <div>
                                    <h3>#<c:out value="${q.quoteRequestId}"/> — <c:out value="${q.productName}"/></h3>
                                    <p><c:out value="${q.companyName}"/> · <c:out value="${q.requestedQuantity}"/> sản phẩm ·
                                        cần trước <c:out value="${q.requiredByText}"/>
                                        <c:if test="${not empty q.preferredShopName}"> · Ưu tiên: <c:out value="${q.preferredShopName}"/></c:if></p>
                                </div>
                                <div>
                                    <b><c:out value="${q.status}"/></b>
                                    <c:if test="${not empty q.quotedAmount}">
                                        <div><fmt:formatNumber value="${q.quotedAmount}" type="number" groupingUsed="true"/>đ</div>
                                    </c:if>
                                    <c:if test="${not empty q.sellerResponseNote}">
                                        <div class="muted"><c:out value="${q.sellerResponseNote}"/></div>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <aside class="order-panel">
            <h2>Gửi yêu cầu mới</h2>
            <form class="stack-form" method="post" action="${pageContext.request.contextPath}/quotes">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <div class="field">
                    <label for="companyName">Tên doanh nghiệp</label>
                    <input id="companyName" type="text" name="companyName" maxlength="180" required>
                </div>
                <div class="field">
                    <label for="productId">Sản phẩm cần in</label>
                    <select id="productId" name="productId" required>
                        <option value="">— Chọn sản phẩm —</option>
                        <c:forEach var="p" items="${products}">
                            <option value="${p.id}"><c:out value="${p.name}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label for="preferredShopId">Gian hàng ưu tiên (không bắt buộc)</label>
                    <select id="preferredShopId" name="preferredShopId">
                        <option value="">— Không cụ thể —</option>
                        <c:forEach var="s" items="${shops}">
                            <option value="${s.id}"><c:out value="${s.name}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label for="quantity">Số lượng yêu cầu</label>
                    <input id="quantity" type="number" name="quantity" min="1" max="1000000" required>
                </div>
                <div class="field">
                    <label for="requiredBy">Cần nhận trước (tùy chọn)</label>
                    <input id="requiredBy" type="date" name="requiredBy">
                </div>
                <button class="button-primary" type="submit">Gửi yêu cầu</button>
            </form>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
