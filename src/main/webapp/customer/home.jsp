<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>PrintKraft — In theo yêu cầu</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=IBM+Plex+Sans:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css?v=20261024-3">
</head>
<body>

<%@ include file="header.jsp" %>

<main id="app">
    <div class="page active" id="page-home">

        <section class="hero">
            <div>
                <div class="hero-eyebrow">Giao mẫu thử trong 48 giờ</div>
                <h1>In ấn của bạn, <span class="accent">theo cách của bạn.</span></h1>
                <p>Tải thiết kế lên, chọn chất liệu và số lượng — xưởng in đối tác của chúng tôi lo phần còn lại. Từ 1 chiếc áo đến 10.000 sản phẩm.</p>
                <div class="hero-cta">
                    <a class="btn-primary" href="${pageContext.request.contextPath}/products">Bắt đầu mua hàng</a>
                    <a class="btn-secondary" href="${pageContext.request.contextPath}/products">Xem sản phẩm mẫu</a>
                </div>
                <div class="hero-stats">
                    <div><strong>4.9/5</strong><span>Đánh giá khách hàng</span></div>
                    <div><strong>120+</strong><span>Xưởng in đối tác</span></div>
                    <div><strong>48h</strong><span>Thời gian sản xuất TB</span></div>
                    <div><strong>63</strong><span>Tỉnh thành giao hàng</span></div>
                </div>
            </div>
            <div class="hero-visual">
                <div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>
                <img src="${pageContext.request.contextPath}/images/mockup_poster.jpg" class="mockup m3" alt="POSTER A2 GLOSS" >
                <img src="${pageContext.request.contextPath}/images/mockup_tshirt.jpg" class="mockup m1" alt="ÁO THUN COTTON 100%" >
                <img src="${pageContext.request.contextPath}/images/mockup_sticker.jpg" class="mockup m4" alt="STICKER DECAL">
            </div>
        </section>

        <%-- Danh mục lấy từ bảng categories --%>
        <div class="cat-strip">
            <div class="inner">
                <c:forEach var="cat" items="${categories}">
                    <c:url value="/products" var="catUrl">
                        <c:param name="category" value="${cat.name}"/>
                    </c:url>
                    <a href="${catUrl}"><c:out value="${cat.name}"/></a>
                </c:forEach>
                <c:if test="${empty categories}">
                    <span class="muted">Chưa có dữu liệu</span>
                </c:if>
            </div>
        </div>

        <section class="section">
            <div class="section-head">
                <div>
                    <div class="eyebrow-line">Khám phá</div>
                    <h2>Sản phẩm nổi bật</h2>
                    <p>Những lựa chọn đang nhận được nhiều sự quan tâm trên PrintKraft.</p>
                </div>
                <a class="link" href="${pageContext.request.contextPath}/products">Xem tất cả →</a>
            </div>
            <c:choose>
                <c:when test="${empty bestsellers}">
                    <div class="empty-state">Chưa có dữ liệu nào.</div>
                </c:when>
                <c:otherwise>
                    <div class="grid">
                        <c:forEach var="product" items="${bestsellers}">
                            <div class="card">
                                <a href="${pageContext.request.contextPath}/product-detail?id=${product.id}">
                                    <div class="card-thumb" style="background:var(--grad-warm);">
                                        <c:if test="${not empty product.tag}"><span class="tag"><c:out value="${product.tag}"/></span></c:if>
                                        <span class="fav">♡</span><c:out value="${product.name}"/>
                                    </div>
                                </a>
                                <div class="card-body">
                                    <h4><c:out value="${product.name}"/></h4>
                                    <div class="meta"><c:out value="${product.printMethod}"/> · Từ <c:out value="${product.minimumQuantity}"/> sản phẩm</div>
                                    <div class="stars"><c:out value="${product.ratingStars}"/><span class="star-off"><c:out value="${product.ratingEmptyStars}"/></span> <span style="color:var(--ink-soft)">(<c:out value="${product.reviewCount}"/>)</span></div>
                                    <div class="price-row" style="margin-top:8px;">
                                        <span class="price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ<small>Giá tham khảo</small></span>
                                        <form class="inline-add js-quick-cart-form" method="post" action="${pageContext.request.contextPath}/cart">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="add">
                                            <input type="hidden" name="shopProductId" value="${product.primaryOffer.shopProductId}">
                                            <input type="hidden" name="productId" value="${product.id}">
                                            <input type="hidden" name="quantity" value="${product.minimumQuantity}">
                                            <button class="add-btn" type="submit"
                                                    title="Thêm ${product.minimumQuantity} sản phẩm vào giỏ hàng"
                                                    aria-label="Thêm <c:out value='${product.name}'/> vào giỏ hàng">+</button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="section" style="padding-top: 20px;">
            <div class="section-head">
                <div>
                    <div class="eyebrow-line">Xu hướng</div>
                    <h2>Gợi ý dành cho bạn</h2>
                    <p>Khám phá thêm các lựa chọn từ những gian hàng trên PrintKraft.</p>
                </div>
                <a class="link" href="${pageContext.request.contextPath}/products">Xem tất cả →</a>
            </div>
            <c:choose>
                <c:when test="${empty trending}">
                    <div class="empty-state">Chưa có dữ liệu sản phẩm.</div>
                </c:when>
                <c:otherwise>
                    <div class="grid">
                        <c:forEach var="product" items="${trending}">
                            <div class="card">
                                <a href="${pageContext.request.contextPath}/product-detail?id=${product.id}">
                                    <div class="card-thumb" style="background:var(--grad-sky);">
                                        <span class="fav">♡</span><c:out value="${product.name}"/>
                                    </div>
                                </a>
                                <div class="card-body">
                                    <h4><c:out value="${product.name}"/></h4>
                                    <div class="meta"><c:out value="${product.printMethod}"/> · Từ <c:out value="${product.minimumQuantity}"/> sản phẩm</div>
                                    <div class="stars"><c:out value="${product.ratingStars}"/><span class="star-off"><c:out value="${product.ratingEmptyStars}"/></span> <span style="color:var(--ink-soft)">(<c:out value="${product.reviewCount}"/>)</span></div>
                                    <div class="price-row" style="margin-top:8px;">
                                        <span class="price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ<small>Giá tham khảo</small></span>
                                        <form class="inline-add js-quick-cart-form" method="post" action="${pageContext.request.contextPath}/cart">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="add">
                                            <input type="hidden" name="shopProductId" value="${product.primaryOffer.shopProductId}">
                                            <input type="hidden" name="productId" value="${product.id}">
                                            <input type="hidden" name="quantity" value="${product.minimumQuantity}">
                                            <button class="add-btn" type="submit"
                                                    title="Thêm ${product.minimumQuantity} sản phẩm vào giỏ hàng"
                                                    aria-label="Thêm <c:out value='${product.name}'/> vào giỏ hàng">+</button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="how">
            <div class="section">
                <div class="section-head">
                    <div>
                        <div class="eyebrow-line" style="color:var(--violet-soft);">Quy trình</div>
                        <h2>Đặt hàng chỉ với 3 bước</h2>
                        <p>Không cần kinh nghiệm thiết kế — công cụ của chúng tôi lo phần khó</p>
                    </div>
                </div>
                <div class="steps">
                    <div class="step">
                        <div class="num">1</div>
                        <h4>Tải thiết kế lên</h4>
                        <p>Dùng công cụ kéo-thả hoặc tải file có sẵn. Xem trước mockup ngay lập tức.</p>
                    </div>
                    <div class="step">
                        <div class="num">2</div>
                        <h4>Chọn xưởng in phù hợp</h4>
                        <p>Hệ thống gợi ý xưởng in gần bạn nhất, đánh giá cao và đúng thời hạn.</p>
                    </div>
                    <div class="step">
                        <div class="num">3</div>
                        <h4>Theo dõi &amp; nhận hàng</h4>
                        <p>Cập nhật trạng thái theo thời gian thực, từ sản xuất đến giao hàng.</p>
                    </div>
                </div>
            </div>
        </section>

    </div>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/quick-cart.js?v=20261024-1"></script>
</body>
</html>
