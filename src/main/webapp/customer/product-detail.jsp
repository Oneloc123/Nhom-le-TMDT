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
                                        <span class="muted" style="margin:0 4px;">·</span>
                                        <a href="#comments"
                                            style="font-size:13px; color:var(--violet); text-decoration:underline;">💬 Hỏi đáp (<c:out value="${commentsCount}"/>)</a>
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

                                <c:if test="${not empty userReviewItem}">
                                    <div style="margin-top:28px;">
                                        <c:choose>
                                            <c:when test="${!userReviewItem.reviewed}">
                                                <div class="review-buyer-cta">
                                                    <div class="review-buyer-cta-text">
                                                        <h3>✨ Bạn đã mua sản phẩm này!</h3>
                                                        <p>Hãy chia sẻ cảm nhận thực tế về chất lượng in ấn và độ hoàn thiện để giúp cộng đồng mua sắm nhé.</p>
                                                    </div>
                                                    <a class="button-primary" href="${pageContext.request.contextPath}/review?item=${userReviewItem.orderItemId}">
                                                        ★ Viết đánh giá ngay
                                                    </a>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="review-buyer-cta" style="background:#f0fdf4; border-color:#86efac;">
                                                    <div class="review-buyer-cta-text">
                                                        <h3 style="color:#166534;">✓ Bạn đã đánh giá sản phẩm này</h3>
                                                        <p style="color:#15803d;">Cảm ơn những đóng góp quý giá của bạn cho cộng đồng PrintKraft!</p>
                                                    </div>
                                                    <a class="button-secondary" href="${pageContext.request.contextPath}/review?item=${userReviewItem.orderItemId}">
                                                        Xem / Chỉnh sửa đánh giá
                                                    </a>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </c:if>

                                <section class="summary-panel" id="reviews" style="margin-top:32px; padding: 28px;">
                                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:12px;">
                                        <h2>Đánh giá từ khách hàng (<c:out value="${ratingSummary.totalReviews}" />)</h2>
                                        <c:if test="${not empty userReviewItem && !userReviewItem.reviewed}">
                                            <a class="button-primary" style="font-size:13px; padding:8px 16px;" href="${pageContext.request.contextPath}/review?item=${userReviewItem.orderItemId}">
                                                ★ Viết đánh giá của bạn
                                            </a>
                                        </c:if>
                                    </div>

                                    <c:if test="${reviewSuccess}">
                                        <div class="notice-ok" style="margin-bottom:20px; font-weight:600;">
                                            🎉 Cảm ơn bạn! Đánh giá sản phẩm của bạn đã được ghi nhận thành công và điểm số đã được cập nhật.
                                        </div>
                                    </c:if>

                                    <c:choose>
                                        <c:when test="${empty reviews}">
                                            <div class="empty-state" style="padding: 36px 20px;">
                                                <div style="font-size: 38px; margin-bottom: 10px;">⭐</div>
                                                <h3>Chưa có đánh giá nào cho sản phẩm này</h3>
                                                <p class="muted" style="margin-top:6px;">Khách hàng sau khi đặt mua có thể để lại đánh giá chất lượng in ấn tại đây.</p>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="rating-summary-card">
                                                <div class="rating-overview-score">
                                                    <div class="rating-huge-num">
                                                        <fmt:formatNumber value="${ratingSummary.averageRating}" maxFractionDigits="1" minFractionDigits="1" />
                                                    </div>
                                                    <div class="rating-huge-stars">
                                                        <c:forEach var="s" begin="1" end="5">
                                                            <span style="color:${s <= ratingSummary.averageRating ? '#f59e0b' : '#d1d5db'}">★</span>
                                                        </c:forEach>
                                                    </div>
                                                    <div class="rating-huge-total">
                                                        ${ratingSummary.totalReviews} lượt đánh giá thực tế
                                                    </div>
                                                </div>

                                                <div class="rating-breakdown-bars">
                                                    <div class="breakdown-row">
                                                        <span class="breakdown-label">5 <span class="star-icon">★</span></span>
                                                        <div class="breakdown-track">
                                                            <div class="breakdown-fill" style="width: ${ratingSummary.percent5}%;"></div>
                                                        </div>
                                                        <span class="breakdown-count">${ratingSummary.star5Count}</span>
                                                    </div>
                                                    <div class="breakdown-row">
                                                        <span class="breakdown-label">4 <span class="star-icon">★</span></span>
                                                        <div class="breakdown-track">
                                                            <div class="breakdown-fill" style="width: ${ratingSummary.percent4}%;"></div>
                                                        </div>
                                                        <span class="breakdown-count">${ratingSummary.star4Count}</span>
                                                    </div>
                                                    <div class="breakdown-row">
                                                        <span class="breakdown-label">3 <span class="star-icon">★</span></span>
                                                        <div class="breakdown-track">
                                                            <div class="breakdown-fill" style="width: ${ratingSummary.percent3}%;"></div>
                                                        </div>
                                                        <span class="breakdown-count">${ratingSummary.star3Count}</span>
                                                    </div>
                                                    <div class="breakdown-row">
                                                        <span class="breakdown-label">2 <span class="star-icon">★</span></span>
                                                        <div class="breakdown-track">
                                                            <div class="breakdown-fill" style="width: ${ratingSummary.percent2}%;"></div>
                                                        </div>
                                                        <span class="breakdown-count">${ratingSummary.star2Count}</span>
                                                    </div>
                                                    <div class="breakdown-row">
                                                        <span class="breakdown-label">1 <span class="star-icon">★</span></span>
                                                        <div class="breakdown-track">
                                                            <div class="breakdown-fill" style="width: ${ratingSummary.percent1}%;"></div>
                                                        </div>
                                                        <span class="breakdown-count">${ratingSummary.star1Count}</span>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="reviews-filter-bar" id="reviewFilterBar">
                                                <span class="reviews-filter-title">Lọc xem:</span>
                                                <button type="button" class="filter-pill active" data-filter="all">Tất cả (${ratingSummary.totalReviews})</button>
                                                <button type="button" class="filter-pill" data-filter="5">5 Sao (${ratingSummary.star5Count})</button>
                                                <button type="button" class="filter-pill" data-filter="4">4 Sao (${ratingSummary.star4Count})</button>
                                                <button type="button" class="filter-pill" data-filter="3">3 Sao (${ratingSummary.star3Count})</button>
                                                <button type="button" class="filter-pill" data-filter="2">2 Sao (${ratingSummary.star2Count})</button>
                                                <button type="button" class="filter-pill" data-filter="1">1 Sao (${ratingSummary.star1Count})</button>
                                            </div>

                                            <div class="review-cards-list" id="reviewListContainer">
                                                <c:forEach var="review" items="${reviews}">
                                                    <article class="review-item-card" data-rating="${review.rating}">
                                                        <div class="reviewer-meta-row">
                                                            <div class="reviewer-info">
                                                                <div class="reviewer-avatar-circle">
                                                                    <c:out value="${review.userInitial}" />
                                                                </div>
                                                                <div class="reviewer-details">
                                                                    <div class="reviewer-name">
                                                                        <c:out value="${review.userDisplayName}" />
                                                                        <c:if test="${not empty sessionScope.currentUser && sessionScope.currentUser.id == review.userId}">
                                                                            <span class="reviewer-you-badge">Bạn</span>
                                                                        </c:if>
                                                                        <span class="verified-buyer-tag">✓ Đã mua hàng</span>
                                                                    </div>
                                                                    <c:if test="${not empty review.reviewDateText}">
                                                                        <div class="review-date-badge">
                                                                            <c:out value="${review.reviewDateText}" />
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
                                                                <span>Gian hàng: <b><c:out value="${review.shopName}" /></b></span>
                                                            </div>
                                                        </c:if>
                                                    </article>
                                                </c:forEach>
                                            </div>

                                            <div id="noFilterMatch" class="empty-state" style="display:none; margin-top:16px;">
                                                Không có đánh giá nào cho mức số sao này.
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </section>


                                <section class="summary-panel" id="comments" style="margin-top:20px; padding: 18px 20px;">
                                    <div class="qa-section-header" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
                                        <div>
                                            <h2>Hỏi đáp & Bình luận sản phẩm (<c:out value="${commentsCount}" />)</h2>
                                            <p>Nơi trao đổi, đặt câu hỏi về quy cách in ấn, chất liệu và giải đáp từ người bán / xưởng in.</p>
                                        </div>
                                        <a class="button-primary" style="font-size:12.5px; padding:6px 14px;" href="#new-comment-box">
                                            ❓ Đặt câu hỏi mới
                                        </a>
                                    </div>

                                    <c:if test="${commentSuccess}">
                                        <div class="notice-ok" style="margin-bottom:12px; font-size:13px; padding:10px 14px; font-weight:600;">
                                             Câu hỏi / bình luận của bạn đã được đăng thành công. Người bán sẽ sớm phản hồi cho bạn!
                                        </div>
                                    </c:if>

                                    <c:if test="${not empty commentError}">
                                        <div class="notice-error" style="margin-bottom:12px; font-size:13px; font-weight:600; background:#fef2f2; color:#b91c1c; border:1px solid #fca5a5; border-radius:var(--r-sm); padding:10px 14px;">
                                            ️ <c:out value="${commentError}"/>
                                        </div>
                                    </c:if>

                                    <div class="qa-new-card" id="new-comment-box">
                                        <c:choose>
                                            <c:when test="${not empty sessionScope.currentUser}">
                                                <div class="qa-user-bar">
                                                    <div class="qa-user-avatar">
                                                        <c:out value="${sessionScope.currentUser.userInitial}"/>
                                                    </div>
                                                    <div class="qa-user-name">
                                                        <c:out value="${sessionScope.currentUser.displayName}"/>
                                                        <c:choose>
                                                            <c:when test="${isProductSeller}">
                                                                <span class="badge-seller-user">🏪 Người bán / Quản trị viên</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="commenter-tag">👤 Khách hàng</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                </div>

                                                <form method="post" action="${pageContext.request.contextPath}/product-comment" id="qaNewForm">
                                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                    <input type="hidden" name="productId" value="${product.id}">

                                                    <div class="feedback-chips-section" style="margin-bottom:8px;">
                                                        <span class="feedback-chips-label">Gợi ý câu hỏi nhanh (chọn để điền):</span>
                                                        <div class="feedback-chips-list">
                                                            <button type="button" class="feedback-chip qa-chip" data-chip="Xưởng in có nhận in số lượng ít không? 📦">Xưởng in có nhận in số lượng ít không? 📦</button>
                                                            <button type="button" class="feedback-chip qa-chip" data-chip="Thời gian in và giao hàng dự kiến là bao lâu? ⏱️">Thời gian in và giao hàng dự kiến là bao lâu? ⏱️</button>
                                                            <button type="button" class="feedback-chip qa-chip" data-chip="Xưởng có hỗ trợ kiểm tra và chỉnh file thiết kế không? 🎨">Xưởng có hỗ trợ kiểm tra file thiết kế không? 🎨</button>
                                                            <button type="button" class="feedback-chip qa-chip" data-chip="Chất liệu in có chống nước và bền màu ngoài trời không? 💧">Chất liệu in có chống nước không? 💧</button>
                                                            <button type="button" class="feedback-chip qa-chip" data-chip="Đơn hàng này có xuất được hóa đơn VAT không shop? 📋">Có xuất hóa đơn VAT không shop? 📋</button>
                                                        </div>
                                                    </div>

                                                    <textarea name="content" id="qaNewInput" class="qa-textarea" rows="2" maxlength="2000" placeholder="Bạn muốn hỏi gì về sản phẩm này? Người bán và cộng đồng sẽ giải đáp thắc mắc cho bạn..." required></textarea>

                                                    <div class="qa-form-footer">
                                                        <span class="qa-form-hint">Bình luận công khai dưới tài khoản của bạn để người bán giải đáp.</span>
                                                        <button type="submit" class="button-primary submit-comment-btn">
                                                            🚀 Gửi câu hỏi / bình luận
                                                        </button>
                                                    </div>
                                                </form>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="inline-comment-login-prompt">
                                                    <div class="login-prompt-icon">💬</div>
                                                    <div class="login-prompt-content">
                                                        <h4>Bạn có thắc mắc hoặc cần tư vấn về sản phẩm này?</h4>
                                                        <p>Đăng nhập bằng tài khoản của bạn để đặt câu hỏi trực tiếp cho người bán và nhận câu trả lời nhanh chóng.</p>
                                                    </div>
                                                    <a class="button-primary" href="${pageContext.request.contextPath}/login?redirect=${pageContext.request.contextPath}/product-detail?id=${product.id}%23new-comment-box">
                                                        Đăng nhập để đặt câu hỏi
                                                    </a>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <c:choose>
                                        <c:when test="${empty comments}">
                                            <div class="empty-state" style="padding: 30px 20px;">
                                                <div style="font-size: 36px; margin-bottom: 10px;">💬</div>
                                                <h3>Chưa có câu hỏi nào về sản phẩm này</h3>
                                                <p class="muted" style="margin-top:6px;">Hãy là người đầu tiên đặt câu hỏi cho xưởng in ở khung phía trên!</p>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="qa-thread-list" id="qaThreadList">
                                                <c:forEach var="comment" items="${comments}" varStatus="loop">
                                                    <div class="qa-thread-card ${loop.count > 3 ? 'qa-thread-card-extra' : ''}" 
                                                         id="comment-${comment.commentId}"
                                                         style="${loop.count > 3 ? 'display: none;' : ''}">
                                                        <div class="qa-thread-header">
                                                            <div class="qa-thread-user">
                                                                <div class="qa-thread-avatar">
                                                                    <c:out value="${comment.userInitial}"/>
                                                                </div>
                                                                <div class="qa-thread-meta">
                                                                    <div class="qa-thread-name">
                                                                        <c:out value="${comment.userDisplayName}"/>
                                                                        <c:if test="${not empty sessionScope.currentUser && sessionScope.currentUser.id == comment.userId}">
                                                                            <span class="reviewer-you-badge">Bạn</span>
                                                                        </c:if>
                                                                    </div>
                                                                    <span class="qa-thread-time"><c:out value="${comment.createdAtText}"/></span>
                                                                </div>
                                                            </div>
                                                            <c:if test="${not empty sessionScope.currentUser && (sessionScope.currentUser.id == comment.userId || sessionScope.currentUser.admin)}">
                                                                <form method="post" action="${pageContext.request.contextPath}/product-comment" style="display:inline;" onsubmit="return confirm('Bạn có chắc chắn muốn xóa câu hỏi này cùng các phản hồi?');">
                                                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                                    <input type="hidden" name="productId" value="${product.id}">
                                                                    <input type="hidden" name="commentId" value="${comment.commentId}">
                                                                    <input type="hidden" name="action" value="delete">
                                                                    <button type="submit" class="qa-delete-btn" title="Xóa câu hỏi">🗑️ Xóa</button>
                                                                </form>
                                                            </c:if>
                                                        </div>

                                                        <div class="qa-question-body">
                                                            <c:out value="${comment.content}"/>
                                                        </div>

                                                        <div class="qa-thread-actions">
                                                            <button type="button" class="qa-reply-toggle-btn" onclick="toggleReplyBox(${comment.commentId})">
                                                                💬 Trả lời
                                                            </button>
                                                            <c:if test="${comment.repliesCount > 0}">
                                                                <button type="button" class="qa-replies-visibility-btn" id="replies-visibility-btn-${comment.commentId}" onclick="toggleAllReplies(${comment.commentId})" title="Ẩn hoặc hiện các câu trả lời">
                                                                    <span class="visibility-icon">👁️</span>
                                                                    <span class="visibility-text">Ẩn <c:out value="${comment.repliesCount}"/> câu trả lời</span>
                                                                </button>
                                                            </c:if>
                                                        </div>

                                                        <c:if test="${not empty comment.replies}">
                                                            <div class="qa-replies-container" id="replies-container-${comment.commentId}">
                                                                <c:forEach var="reply" items="${comment.replies}" varStatus="rLoop">
                                                                    <div class="qa-reply-item ${reply.sellerReply ? 'is-seller' : ''} ${rLoop.count > 1 ? 'qa-reply-extra' : ''}" 
                                                                         id="reply-${reply.commentId}"
                                                                         style="${rLoop.count > 1 ? 'display: none;' : ''}">
                                                                        <div class="qa-reply-header">
                                                                            <div class="qa-reply-user">
                                                                                <div class="qa-reply-avatar">
                                                                                    <c:out value="${reply.userInitial}"/>
                                                                                </div>
                                                                                <div class="qa-reply-name">
                                                                                    <c:out value="${reply.userDisplayName}"/>
                                                                                    <c:choose>
                                                                                        <c:when test="${reply.sellerReply}">
                                                                                            <span class="badge-seller-reply">✓ Người bán phản hồi</span>
                                                                                        </c:when>
                                                                                        <c:when test="${not empty sessionScope.currentUser && sessionScope.currentUser.id == reply.userId}">
                                                                                            <span class="reviewer-you-badge">Bạn</span>
                                                                                        </c:when>
                                                                                    </c:choose>
                                                                                </div>
                                                                            </div>
                                                                            <div style="display:flex; align-items:center; gap:8px;">
                                                                                <span class="qa-thread-time"><c:out value="${reply.createdAtText}"/></span>
                                                                                <c:if test="${not empty sessionScope.currentUser && (sessionScope.currentUser.id == reply.userId || sessionScope.currentUser.admin)}">
                                                                                    <form method="post" action="${pageContext.request.contextPath}/product-comment" style="display:inline;" onsubmit="return confirm('Bạn có chắc muốn xóa phản hồi này?');">
                                                                                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                                                        <input type="hidden" name="productId" value="${product.id}">
                                                                                        <input type="hidden" name="commentId" value="${reply.commentId}">
                                                                                        <input type="hidden" name="action" value="delete">
                                                                                        <button type="submit" class="qa-delete-btn" title="Xóa phản hồi">🗑️</button>
                                                                                    </form>
                                                                                </c:if>
                                                                            </div>
                                                                        </div>
                                                                        <div class="qa-reply-text">
                                                                            <c:out value="${reply.content}"/>
                                                                        </div>
                                                                    </div>
                                                                </c:forEach>
                                                                <c:if test="${comment.repliesCount > 1}">
                                                                    <button type="button" class="qa-replies-more-btn" id="replies-more-btn-${comment.commentId}" onclick="toggleRepliesMore(${comment.commentId}, ${comment.repliesCount - 1})">
                                                                        <span class="reply-toggle-icon">▾</span>
                                                                        <span class="reply-toggle-text">Xem thêm <c:out value="${comment.repliesCount - 1}"/> câu trả lời khác</span>
                                                                    </button>
                                                                </c:if>
                                                            </div>
                                                        </c:if>

                                                        <div class="qa-inline-reply-box" id="reply-box-${comment.commentId}" style="display:none;">
                                                            <c:choose>
                                                                <c:when test="${not empty sessionScope.currentUser}">
                                                                    <form method="post" action="${pageContext.request.contextPath}/product-comment">
                                                                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                                        <input type="hidden" name="productId" value="${product.id}">
                                                                        <input type="hidden" name="parentId" value="${comment.commentId}">
                                                                        <textarea name="content" rows="2" maxlength="2000" placeholder="${isProductSeller ? 'Phản hồi khách hàng với tư cách Người bán / Xưởng in...' : 'Nhập câu trả lời hoặc trao đổi thêm...'}" required></textarea>
                                                                        <div class="qa-reply-actions">
                                                                            <button type="button" class="button-secondary" style="font-size:12.5px; padding:6px 14px;" onclick="toggleReplyBox(${comment.commentId})">Hủy</button>
                                                                            <button type="submit" class="button-primary" style="font-size:12.5px; padding:6px 16px;">
                                                                                ${isProductSeller ? '📢 Gửi phản hồi (Người bán)' : 'Gửi trả lời'}
                                                                            </button>
                                                                        </div>
                                                                    </form>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <p class="muted" style="margin:0; font-size:13px;">
                                                                        Vui lòng <a href="${pageContext.request.contextPath}/login?redirect=${pageContext.request.contextPath}/product-detail?id=${product.id}%23comment-${comment.commentId}" style="color:var(--violet); font-weight:600;">Đăng nhập</a> để gửi câu trả lời.
                                                                    </p>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </div>
                                                    </div>
                                                </c:forEach>
                                            </div>

                                            <c:if test="${fn:length(comments) > 3}">
                                                <div class="qa-collapse-control">
                                                    <button type="button" class="qa-toggle-collapse-btn" id="qaToggleCollapseBtn" onclick="toggleCommentsCollapse(${fn:length(comments)})">
                                                        <span class="qa-btn-icon">▾</span>
                                                        <span class="qa-btn-text">Xem thêm bình luận (còn <c:out value="${fn:length(comments) - 3}"/> câu hỏi)</span>
                                                    </button>
                                                    <div class="qa-collapse-status-text" id="qaCollapseStatusText">
                                                        Đang hiển thị 3 trên tổng số <c:out value="${fn:length(comments)}"/> bình luận
                                                    </div>
                                                </div>
                                            </c:if>
                                        </c:otherwise>
                                    </c:choose>
                                </section>

                                <script>
                                    (function () {
                                        const filterBar = document.getElementById('reviewFilterBar');
                                        const container = document.getElementById('reviewListContainer');
                                        const noMatch = document.getElementById('noFilterMatch');
                                        if (filterBar && container) {
                                            const filterBtns = filterBar.querySelectorAll('.filter-pill');
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
                                        }
                                        const qaChips = document.querySelectorAll('.qa-chip');
                                        const qaInput = document.getElementById('qaNewInput');
                                        if (qaChips && qaInput) {
                                            qaChips.forEach(chip => {
                                                chip.addEventListener('click', function () {
                                                    const text = this.getAttribute('data-chip');
                                                    if (!qaInput.value.trim()) {
                                                        qaInput.value = text;
                                                    } else if (!qaInput.value.includes(text)) {
                                                        qaInput.value = qaInput.value.trim() + ' ' + text;
                                                    }
                                                    this.classList.add('used');
                                                    qaInput.focus();
                                                });
                                            });
                                        }
                                    })();

                                    function toggleReplyBox(commentId) {
                                        const box = document.getElementById('reply-box-' + commentId);
                                        if (!box) return;
                                        if (box.style.display === 'none' || box.style.display === '') {
                                            box.style.display = 'block';
                                            const ta = box.querySelector('textarea');
                                            if (ta) ta.focus();
                                        } else {
                                            box.style.display = 'none';
                                        }
                                    }

                                    function toggleCommentsCollapse(totalCount) {
                                        const btn = document.getElementById('qaToggleCollapseBtn');
                                        const statusText = document.getElementById('qaCollapseStatusText');
                                        const extraCards = document.querySelectorAll('.qa-thread-card-extra');
                                        if (!btn || extraCards.length === 0) return;

                                        const isExpanded = btn.classList.contains('is-expanded');
                                        const remainingCount = extraCards.length;
                                        const total = totalCount || document.querySelectorAll('.qa-thread-card').length;

                                        if (!isExpanded) {
                                            extraCards.forEach(card => {
                                                card.style.display = 'block';
                                            });
                                            btn.classList.add('is-expanded');
                                            btn.innerHTML = '<span class="qa-btn-icon">▴</span> <span class="qa-btn-text">Ẩn bớt bình luận</span>';
                                            if (statusText) {
                                                statusText.textContent = 'Đang hiển thị tất cả ' + total + ' bình luận';
                                            }
                                        } else {
                                            extraCards.forEach(card => {
                                                card.style.display = 'none';
                                            });
                                            btn.classList.remove('is-expanded');
                                            btn.innerHTML = '<span class="qa-btn-icon">▾</span> <span class="qa-btn-text">Xem thêm bình luận (còn ' + remainingCount + ' câu hỏi)</span>';
                                            if (statusText) {
                                                statusText.textContent = 'Đang hiển thị 3 trên tổng số ' + total + ' bình luận';
                                            }
                                            const section = document.getElementById('comments');
                                            if (section) {
                                                const headerOffset = 80;
                                                const elementPosition = section.getBoundingClientRect().top;
                                                const offsetPosition = elementPosition + window.pageYOffset - headerOffset;
                                                window.scrollTo({
                                                    top: offsetPosition,
                                                    behavior: 'smooth'
                                                });
                                            }
                                        }
                                    }
                                    function toggleRepliesMore(commentId, extraCount) {
                                        const container = document.getElementById('replies-container-' + commentId);
                                        const btn = document.getElementById('replies-more-btn-' + commentId);
                                        if (!container || !btn) return;

                                        const extraReplies = container.querySelectorAll('.qa-reply-extra');
                                        const isExpanded = btn.classList.contains('is-expanded');

                                        if (!isExpanded) {
                                            extraReplies.forEach(reply => {
                                                reply.style.display = 'block';
                                            });
                                            btn.classList.add('is-expanded');
                                            btn.innerHTML = '<span class="reply-toggle-icon">▴</span> <span class="reply-toggle-text">Thu gọn câu trả lời</span>';
                                        } else {
                                            extraReplies.forEach(reply => {
                                                reply.style.display = 'none';
                                            });
                                            btn.classList.remove('is-expanded');
                                            btn.innerHTML = '<span class="reply-toggle-icon">▾</span> <span class="reply-toggle-text">Xem thêm ' + extraCount + ' câu trả lời khác</span>';
                                        }
                                    }
                                    function toggleAllReplies(commentId) {
                                        const container = document.getElementById('replies-container-' + commentId);
                                        const btn = document.getElementById('replies-visibility-btn-' + commentId);
                                        if (!container || !btn) return;

                                        const isHidden = container.style.display === 'none';
                                        const totalCount = container.querySelectorAll('.qa-reply-item').length;

                                        if (isHidden) {
                                            container.style.display = 'grid';
                                            btn.classList.remove('is-hidden');
                                            btn.innerHTML = '<span class="visibility-icon">👁️</span> <span class="visibility-text">Ẩn ' + totalCount + ' câu trả lời</span>';
                                        } else {
                                            container.style.display = 'none';
                                            btn.classList.add('is-hidden');
                                            btn.innerHTML = '<span class="visibility-icon">👁️‍🗨️</span> <span class="visibility-text">Hiện ' + totalCount + ' câu trả lời</span>';
                                        }
                                    }
                                    document.addEventListener('DOMContentLoaded', function () {
                                        const hash = window.location.hash;
                                        if (hash && (hash.startsWith('#comment-') || hash.startsWith('#reply-'))) {
                                            const targetEl = document.querySelector(hash);
                                            if (targetEl) {
                                                const parentExtraCard = targetEl.closest('.qa-thread-card-extra');
                                                if (parentExtraCard) {
                                                    const btn = document.getElementById('qaToggleCollapseBtn');
                                                    if (btn && !btn.classList.contains('is-expanded')) {
                                                        toggleCommentsCollapse();
                                                    }
                                                }
                                                const parentExtraReply = targetEl.closest('.qa-reply-extra');
                                                if (parentExtraReply) {
                                                    const parentContainer = targetEl.closest('.qa-replies-container');
                                                    if (parentContainer) {
                                                        const moreBtn = parentContainer.querySelector('.qa-replies-more-btn');
                                                        if (moreBtn && !moreBtn.classList.contains('is-expanded')) {
                                                            moreBtn.click();
                                                        }
                                                    }
                                                }
                                                setTimeout(() => {
                                                    targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                                }, 150);
                                            }
                                        }
                                    });
                                </script>
                        </main>
                        <%@ include file="footer.jsp" %>
                </body>

                </html>