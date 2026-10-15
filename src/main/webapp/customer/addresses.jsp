<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sổ địa chỉ - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Tài khoản</div>
            <h1>Sổ địa chỉ nhận hàng</h1>
            <p>Quản lý địa chỉ giao hàng; địa chỉ mặc định sẽ được chọn sẵn khi đặt hàng.</p>
        </div>
        <a class="button-secondary" href="${pageContext.request.contextPath}/profile">Quay lại hồ sơ</a>
    </div>

    <c:if test="${param.saved == '1'}">
        <div class="notice-ok">Đã lưu địa chỉ.</div>
    </c:if>
    <c:if test="${param.deleted == '1'}">
        <div class="notice-ok">Đã xóa địa chỉ.</div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="notice-error"><c:out value="${param.error}"/></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="summary-panel">
            <h2>Địa chỉ đã lưu</h2>
            <c:choose>
                <c:when test="${empty addresses}">
                    <div class="empty-state">Chưa có địa chỉ nào. Thêm địa chỉ ở biểu mẫu bên phải.</div>
                </c:when>
                <c:otherwise>
                    <div class="cart-list">
                        <c:forEach var="addr" items="${addresses}">
                            <div class="cart-line">
                                <div>
                                    <h3><c:out value="${addr.recipientName}"/> — <c:out value="${addr.recipientPhone}"/>
                                        <c:if test="${addr.defaultAddress}"><span class="product-tag">Mặc định</span></c:if>
                                    </h3>
                                    <p><c:out value="${addr.fullLine}"/></p>
                                </div>
                                <form method="post" action="${pageContext.request.contextPath}/addresses">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="setDefault">
                                    <input type="hidden" name="addressId" value="${addr.addressId}">
                                    <button class="button-secondary" type="submit" ${addr.defaultAddress ? 'disabled' : ''}>Đặt mặc định</button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/addresses"
                                      onsubmit="return confirm('Xóa địa chỉ này?');">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="addressId" value="${addr.addressId}">
                                    <button class="button-danger" type="submit">Xóa</button>
                                </form>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <aside class="order-panel">
            <h2>Thêm địa chỉ mới</h2>
            <form class="stack-form" method="post" action="${pageContext.request.contextPath}/addresses">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="add">
                <div class="field">
                    <label for="recipientName">Tên người nhận</label>
                    <input id="recipientName" type="text" name="recipientName" required maxlength="120">
                </div>
                <div class="field">
                    <label for="recipientPhone">Số điện thoại</label>
                    <input id="recipientPhone" type="text" name="recipientPhone" required pattern="[0-9]{9,12}">
                </div>
                <div class="field">
                    <label for="province">Tỉnh / Thành phố</label>
                    <input id="province" type="text" name="province" required maxlength="100">
                </div>
                <div class="field">
                    <label for="district">Quận / Huyện</label>
                    <input id="district" type="text" name="district" required maxlength="100">
                </div>
                <div class="field">
                    <label for="addressLine">Địa chỉ cụ thể</label>
                    <input id="addressLine" type="text" name="addressLine" required maxlength="255">
                </div>
                <div class="field">
                    <label><input type="checkbox" name="defaultAddress" value="true"> Đặt làm địa chỉ mặc định</label>
                </div>
                <button class="button-primary" type="submit">Thêm địa chỉ</button>
            </form>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
