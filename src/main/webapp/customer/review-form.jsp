<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đánh giá sản phẩm - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Đánh giá</div>
            <h1>Đánh giá sản phẩm đã mua</h1>
            <p><c:out value="${item.productName}"/></p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="form-panel">
            <form class="stack-form" method="post" action="${pageContext.request.contextPath}/review">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="item" value="${item.orderItemId}">
                <div class="field">
                    <span>Điểm đánh giá</span>
                    <select name="rating" required>
                        <option value="5">★★★★★ — Rất hài lòng</option>
                        <option value="4">★★★★ — Hài lòng</option>
                        <option value="3">★★★ — Bình thường</option>
                        <option value="2">★★ — Chưa hài lòng</option>
                        <option value="1">★ — Rất không hài lòng</option>
                    </select>
                </div>
                <div class="field">
                    <label for="reviewText">Nội dung đánh giá</label>
                    <textarea id="reviewText" name="reviewText" rows="5" maxlength="2000" required
                              placeholder="Chia sẻ trải nghiệm về chất lượng in, thời gian giao..."></textarea>
                </div>
                <div class="button-row">
                    <button class="button-primary" type="submit">Gửi đánh giá</button>
                    <a class="button-secondary" href="${pageContext.request.contextPath}/product-detail?id=${item.productId}">Quay lại sản phẩm</a>
                </div>
            </form>
        </section>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
