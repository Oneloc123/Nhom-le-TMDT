<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
                <!DOCTYPE html>
                <html lang="vi">

                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>
                        <c:out value="${product.name}" /> - PrintKraft
                    </title>
                    <%-- tiêu đề đã encode bằng c:out --%>
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
                </head>

                <body>
                    <%@ include file="header.jsp" %>

                        <main class="shop-page">
                            <div class="detail-layout">
                                <div class="detail-visual product-visual ${product.visualClass}">
                                    <c:choose>
                                        <c:when test="${not empty product.imageUrl}">
                                            <img src="<c:out value='${product.imageUrl}'/>"
                                                alt="<c:out value='${product.name}'/>"
                                                style="max-width:100%; max-height:100%; object-fit:contain;">
                                        </c:when>
                                        <c:otherwise>
                                            <span>
                                                <c:out value="${product.name}" />
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <section class="detail-copy">
                                    <div class="eyebrow-line">
                                        <c:out value="${product.categoryName}" />
                                    </div>
                                    <h1>
                                        <c:out value="${product.name}" />
                                    </h1>
                                    <div class="rating">
                                        <span style="color:#f59e0b; font-size:16px;">
                                            <c:forEach var="i" begin="1" end="5">
                                                <c:choose>
                                                    <c:when test="${i <= product.rating}">★</c:when>
                                                    <c:otherwise><span style="color:#d1d5db;">★</span></c:otherwise>
                                                </c:choose>
                                            </c:forEach>
                                        </span>
                                        <span class="muted">
                                            <c:out value="${product.rating}" />/5 ·
                                            <c:out value="${product.reviewCount}" /> đánh giá
                                        </span>
                                        <a href="#reviews"
                                            style="font-size:13px; color:var(--violet); text-decoration:underline; margin-left:8px;">Xem
                                            đánh giá</a>
                                    </div>
                                    <p class="description">
                                        <c:out value="${product.description}" />
                                    </p>
                                    <div class="detail-price">
                                        <fmt:formatNumber value="${product.price}" type="number" groupingUsed="true" />đ
                                    </div>
                                    <ul class="spec-list">
                                        <li><span>Phương pháp in</span><b>
                                                <c:out value="${product.printMethod}" />
                                            </b></li>
                                        <li><span>Số lượng tối thiểu</span><b>
                                                <c:out value="${product.minimumQuantity}" /> sản phẩm
                                            </b></li>
                                        <li><span>Gian hàng</span><b>
                                                <c:out value="${product.storeName}" />
                                            </b></li>
                                        <li><span>Đơn vị tính</span><b>
                                                <c:out value="${product.baseUnit}" />
                                            </b></li>
                                    </ul>
                                    <form class="button-row" method="post"
                                        action="${pageContext.request.contextPath}/cart">
                                        <input type="hidden" name="csrfToken"
                                            value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                        <input type="hidden" name="action" value="add">
                                        <c:choose>
                                            <c:when test="${empty product.offers}">
                                                <p class="muted">Sản phẩm hiện chưa có gian hàng nhận in.</p>
                                            </c:when>
                                            <c:otherwise>
                                                <input type="hidden" name="productId" value="${product.id}">
                                                <label class="field" style="margin:0; width:100px;">
                                                    <span>Số lượng</span>
                                                    <input class="quantity-input" type="number" name="quantity"
                                                        min="${product.minimumQuantity}"
                                                        value="${product.minimumQuantity}">
                                                </label>
                                                <button class="button-primary" type="submit">Thêm vào giỏ hàng</button>
                                            </c:otherwise>
                                        </c:choose>
                                        <a class="button-secondary"
                                            href="${pageContext.request.contextPath}/products">Tiếp tục mua</a>
                                    </form>
                                    <%-- Yêu thích: thêm/bỏ lưu theo tài khoản hiện tại --%>
                                        <c:choose>
                                            <c:when test="${empty sessionScope.currentUser}">
                                                <p class="muted"><a href="${pageContext.request.contextPath}/login">Đăng
                                                        nhập</a> để lưu sản phẩm yêu thích.</p>
                                            </c:when>
                                            <c:when test="${favorite}">
                                                <form method="post"
                                                    action="${pageContext.request.contextPath}/favorites"
                                                    style="margin-top:10px;">
                                                    <input type="hidden" name="csrfToken"
                                                        value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                    <input type="hidden" name="action" value="remove">
                                                    <input type="hidden" name="productId" value="${product.id}">
                                                    <button class="button-secondary" type="submit">♥ Đang lưu yêu
                                                        thích</button>
                                                </form>
                                            </c:when>
                                            <c:otherwise>
                                                <form method="post"
                                                    action="${pageContext.request.contextPath}/favorites"
                                                    style="margin-top:10px;">
                                                    <input type="hidden" name="csrfToken"
                                                        value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                    <input type="hidden" name="action" value="add">
                                                    <input type="hidden" name="productId" value="${product.id}">
                                                    <button class="button-secondary" type="submit">♡ Thêm vào yêu
                                                        thích</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                </section>
                            </div>

                            <%-- Các chào bán (gian hàng) cho sản phẩm này --%>
                                <c:if test="${not empty product.offers}">
                                    <section class="summary-panel" style="margin-top:32px;">
                                        <h2>Lựa chọn gian hàng (
                                            <c:out value="${fn:length(product.offers)}" />)
                                        </h2>
                                        <p class="muted">Mỗi gian hàng có riêng giá, vật liệu và thời gian sản xuất.
                                            Chọn chào bán phù hợp để thêm vào giỏ.</p>
                                        <div class="cart-list">
                                            <c:forEach var="offer" items="${product.offers}">
                                                <div class="cart-line">
                                                    <div>
                                                        <h3>
                                                            <c:out value="${offer.shopName}" />
                                                        </h3>
                                                        <p>In
                                                            <c:out value="${offer.printingMethod}" /> ·
                                                            <c:out value="${offer.material}" /> ·
                                                            từ
                                                            <c:out value="${offer.minimumQuantity}" /> sản phẩm ·
                                                            <c:out value="${offer.productionDays}" /> ngày sản xuất
                                                        </p>
                                                    </div>
                                                    <strong>
                                                        <fmt:formatNumber value="${offer.unitPrice}" type="number"
                                                            groupingUsed="true" />đ
                                                    </strong>
                                                    <c:choose>
                                                        <c:when test="${offer.purchasable}">
                                                            <form method="post"
                                                                action="${pageContext.request.contextPath}/cart">
                                                                <input type="hidden" name="csrfToken"
                                                                    value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                                <input type="hidden" name="action" value="add">
                                                                <input type="hidden" name="shopProductId"
                                                                    value="${offer.shopProductId}">
                                                                <input class="quantity-input" type="number"
                                                                    name="quantity" min="${offer.minimumQuantity}"
                                                                    value="${offer.minimumQuantity}"
                                                                    style="width:80px;">
                                                                <button class="button-primary"
                                                                    type="submit">Thêm</button>
                                                            </form>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="muted">Tạm hết hàng</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </section>
                                </c:if>

                                <%-- Thông báo khi vừa gửi đánh giá xong --%>
                                    <c:if test="${reviewSuccess}">
                                        <div class="notice-ok" style="margin-top:28px; font-weight:600;">
                                            🎉 Cảm ơn bạn! Đánh giá của bạn đã được đăng thành công và điểm số sản phẩm
                                            đã được cập nhật.
                                        </div>
                                    </c:if>

                                    <%-- Banner kêu gọi đánh giá nếu khách đăng nhập đã mua sản phẩm này --%>
                                        <c:if test="${not empty userReviewItem}">
                                            <div style="margin-top:28px;">
                                                <c:choose>
                                                    <c:when test="${!userReviewItem.reviewed}">
                                                        <div class="review-buyer-cta">
                                                            <div class="review-buyer-cta-text">
                                                                <h3>✨ Bạn đã mua sản phẩm này!</h3>
                                                                <p>Hãy chia sẻ cảm nhận thực tế về chất lượng in ấn và
                                                                    độ hoàn thiện để giúp cộng đồng nhé.</p>
                                                            </div>
                                                            <a class="button-primary"
                                                                href="${pageContext.request.contextPath}/review?item=${userReviewItem.orderItemId}">
                                                                ★ Viết đánh giá ngay
                                                            </a>
                                                        </div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <div class="review-buyer-cta"
                                                            style="background:#f0fdf4; border-color:#86efac;">
                                                            <div class="review-buyer-cta-text">
                                                                <h3 style="color:#166534;">✓ Bạn đã đánh giá sản phẩm
                                                                    này</h3>
                                                                <p style="color:#15803d;">Cảm ơn những đóng góp quý giá
                                                                    của bạn cho cộng đồng PrintKraft!</p>
                                                            </div>
                                                            <a class="button-secondary"
                                                                href="${pageContext.request.contextPath}/review?item=${userReviewItem.orderItemId}">
                                                                Xem / Chỉnh sửa đánh giá
                                                            </a>
                                                        </div>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:if>

                                        <%-- Khu vực Đánh giá sản phẩm chi tiết --%>
                                            <section class="summary-panel" id="reviews"
                                                style="margin-top:32px; padding: 28px;">
                                                <div
                                                    style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:12px;">
                                                    <h2>Đánh giá từ khách hàng (
                                                        <c:out value="${ratingSummary.totalReviews}" />)
                                                    </h2>
                                                    <c:if
                                                        test="${not empty userReviewItem && !userReviewItem.reviewed}">
                                                        <a class="button-primary"
                                                            style="font-size:13px; padding:8px 16px;"
                                                            href="${pageContext.request.contextPath}/review?item=${userReviewItem.orderItemId}">
                                                            ★ Viết đánh giá của bạn
                                                        </a>
                                                    </c:if>
                                                </div>

                                                <c:choose>
                                                    <c:when test="${empty reviews}">
                                                        <div class="empty-state" style="padding: 40px 20px;">
                                                            <div style="font-size: 40px; margin-bottom: 12px;">⭐</div>
                                                            <h3>Chưa có đánh giá nào cho sản phẩm này</h3>
                                                            <p class="muted" style="margin-top:6px;">Hãy là người đầu
                                                                tiên mua và chia sẻ trải nghiệm in ấn của bạn!</p>
                                                        </div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <!-- Thẻ tổng quan điểm đánh giá và biểu đồ phân bổ -->
                                                        <div class="rating-summary-card">
                                                            <div class="rating-overview-score">
                                                                <div class="rating-huge-num">
                                                                    <fmt:formatNumber
                                                                        value="${ratingSummary.averageRating}"
                                                                        maxFractionDigits="1" minFractionDigits="1" />
                                                                </div>
                                                                <div class="rating-huge-stars">
                                                                    <c:forEach var="s" begin="1" end="5">
                                                                        <span
                                                                            style="color:${s <= ratingSummary.averageRating ? '#f59e0b' : '#d1d5db'}">★</span>
                                                                    </c:forEach>
                                                                </div>
                                                                <div class="rating-huge-total">
                                                                    ${ratingSummary.totalReviews} lượt đánh giá thực tế
                                                                </div>
                                                            </div>

                                                            <div class="rating-breakdown-bars">
                                                                <div class="breakdown-row">
                                                                    <span class="breakdown-label">5 <span
                                                                            class="star-icon">★</span></span>
                                                                    <div class="breakdown-track">
                                                                        <div class="breakdown-fill"
                                                                            style="width: ${ratingSummary.percent5}%;">
                                                                        </div>
                                                                    </div>
                                                                    <span
                                                                        class="breakdown-count">${ratingSummary.star5Count}</span>
                                                                </div>
                                                                <div class="breakdown-row">
                                                                    <span class="breakdown-label">4 <span
                                                                            class="star-icon">★</span></span>
                                                                    <div class="breakdown-track">
                                                                        <div class="breakdown-fill"
                                                                            style="width: ${ratingSummary.percent4}%;">
                                                                        </div>
                                                                    </div>
                                                                    <span
                                                                        class="breakdown-count">${ratingSummary.star4Count}</span>
                                                                </div>
                                                                <div class="breakdown-row">
                                                                    <span class="breakdown-label">3 <span
                                                                            class="star-icon">★</span></span>
                                                                    <div class="breakdown-track">
                                                                        <div class="breakdown-fill"
                                                                            style="width: ${ratingSummary.percent3}%;">
                                                                        </div>
                                                                    </div>
                                                                    <span
                                                                        class="breakdown-count">${ratingSummary.star3Count}</span>
                                                                </div>
                                                                <div class="breakdown-row">
                                                                    <span class="breakdown-label">2 <span
                                                                            class="star-icon">★</span></span>
                                                                    <div class="breakdown-track">
                                                                        <div class="breakdown-fill"
                                                                            style="width: ${ratingSummary.percent2}%;">
                                                                        </div>
                                                                    </div>
                                                                    <span
                                                                        class="breakdown-count">${ratingSummary.star2Count}</span>
                                                                </div>
                                                                <div class="breakdown-row">
                                                                    <span class="breakdown-label">1 <span
                                                                            class="star-icon">★</span></span>
                                                                    <div class="breakdown-track">
                                                                        <div class="breakdown-fill"
                                                                            style="width: ${ratingSummary.percent1}%;">
                                                                        </div>
                                                                    </div>
                                                                    <span
                                                                        class="breakdown-count">${ratingSummary.star1Count}</span>
                                                                </div>
                                                            </div>
                                                        </div>

                                                        <!-- Bộ lọc theo số sao -->
                                                        <div class="reviews-filter-bar" id="reviewFilterBar">
                                                            <span class="reviews-filter-title">Lọc xem:</span>
                                                            <button type="button" class="filter-pill active"
                                                                data-filter="all">Tất cả
                                                                (${ratingSummary.totalReviews})</button>
                                                            <button type="button" class="filter-pill" data-filter="5">5
                                                                Sao (${ratingSummary.star5Count})</button>
                                                            <button type="button" class="filter-pill" data-filter="4">4
                                                                Sao (${ratingSummary.star4Count})</button>
                                                            <button type="button" class="filter-pill" data-filter="3">3
                                                                Sao (${ratingSummary.star3Count})</button>
                                                            <button type="button" class="filter-pill" data-filter="2">2
                                                                Sao (${ratingSummary.star2Count})</button>
                                                            <button type="button" class="filter-pill" data-filter="1">1
                                                                Sao (${ratingSummary.star1Count})</button>
                                                        </div>

                                                        <!-- Danh sách từng đánh giá -->
                                                        <div class="review-cards-list" id="reviewListContainer">
                                                            <c:forEach var="review" items="${reviews}">
                                                                <article class="review-item-card"
                                                                    data-rating="${review.rating}">
                                                                    <div class="reviewer-meta-row">
                                                                        <div class="reviewer-info">
                                                                            <div class="reviewer-avatar-circle">
                                                                                <c:out value="${review.userInitial}" />
                                                                            </div>
                                                                            <div class="reviewer-details">
                                                                                <div class="reviewer-name">
                                                                                    <c:out
                                                                                        value="${review.userDisplayName}" />
                                                                                    <span class="verified-buyer-tag">✓
                                                                                        Đã mua hàng</span>
                                                                                </div>
                                                                                <c:if
                                                                                    test="${not empty review.reviewDateText}">
                                                                                    <div class="review-date-badge">
                                                                                        <c:out
                                                                                            value="${review.reviewDateText}" />
                                                                                    </div>
                                                                                </c:if>
                                                                            </div>
                                                                        </div>
                                                                    </div>

                                                                    <div class="review-stars-display">
                                                                        <span class="gold-stars">
                                                                            <c:out value="${review.stars}" />
                                                                        </span><span class="gray-stars">
                                                                            <c:out value="${review.emptyStars}" />
                                                                        </span>
                                                                        <span class="rating-tag">
                                                                            <c:out value="${review.ratingLabel}" />
                                                                        </span>
                                                                    </div>

                                                                    <div class="review-body-text">
                                                                        <c:out value="${review.reviewText}" />
                                                                    </div>

                                                                    <c:if test="${not empty review.shopName}">
                                                                        <div class="review-extra-info">
                                                                            <span>Gian hàng: <b>
                                                                                    <c:out value="${review.shopName}" />
                                                                                </b></span>
                                                                        </div>
                                                                    </c:if>
                                                                </article>
                                                            </c:forEach>
                                                        </div>

                                                        <div id="noFilterMatch" class="empty-state"
                                                            style="display:none; margin-top:16px;">
                                                            Không có đánh giá nào cho mức số sao này.
                                                        </div>
                                                    </c:otherwise>
                                                </c:choose>
                                            </section>

                                            <script>
                                                (function () {
                                                    const filterBar = document.getElementById('reviewFilterBar');
                                                    if (!filterBar) return;
                                                    const filterBtns = filterBar.querySelectorAll('.filter-pill');
                                                    const container = document.getElementById('reviewListContainer');
                                                    const noMatch = document.getElementById('noFilterMatch');
                                                    if (!container) return;
                                                    const cards = container.querySelectorAll('.review-item-card');

                                                    filterBtns.forEach(btn => {
                                                        btn.addEventListener('click', function () {
                                                            filterBtns.forEach(b => b.classList.remove('active'));
                                                            this.classList.add('active');
                                                            const filter = this.getAttribute('data-filter');
                                                            let visibleCount = 0;

                                                            cards.forEach(card => {
                                                                const rating = card.getAttribute('data-rating');
                                                                if (filter === 'all' || rating === filter) {
                                                                    card.style.display = '';
                                                                    visibleCount++;
                                                                } else {
                                                                    card.style.display = 'none';
                                                                }
                                                            });

                                                            if (noMatch) {
                                                                noMatch.style.display = visibleCount === 0 ? 'block' : 'none';
                                                            }
                                                        });
                                                    });
                                                })();
                                            </script>
                        </main>
                        <%@ include file="footer.jsp" %>
                </body>

                </html>