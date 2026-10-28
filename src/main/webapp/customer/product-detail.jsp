<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${product.name}"/> - PrintKraft</title>
    <%-- tiêu đề đã encode bằng c:out --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css?v=20261024-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261026-1">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page product-detail-page">
    <div id="productToast" class="product-toast" role="status" aria-live="polite" aria-atomic="true"></div>
    <div class="detail-layout product-hero-card">
        <div class="product-gallery">
        <div class="detail-visual product-visual ${product.visualClass}">
            <c:choose>
                <c:when test="${not empty product.imageUrl}">
                    <img id="mainProductImage" src="<c:out value='${product.imageUrl}'/>" alt="<c:out value='${product.name}'/>"
                         style="max-width:100%; max-height:100%; object-fit:contain;">
                </c:when>
                <c:otherwise>
                    <span><c:out value="${product.name}"/></span>
                </c:otherwise>
            </c:choose>
        </div>
        <c:if test="${fn:length(product.images) > 1}">
            <div class="product-thumbnails" aria-label="Thư viện ảnh sản phẩm">
                <c:forEach var="image" items="${product.images}" varStatus="status">
                    <button type="button" class="product-thumbnail ${status.first ? 'active' : ''}"
                            data-image="<c:out value='${image.imageUrl}'/>"
                            data-alt="<c:out value='${empty image.altText ? product.name : image.altText}'/>">
                        <img src="<c:out value='${image.imageUrl}'/>"
                             alt="<c:out value='${empty image.altText ? product.name : image.altText}'/>">
                    </button>
                </c:forEach>
            </div>
        </c:if>
        </div>
        <section class="detail-copy">
            <div class="eyebrow-line"><c:out value="${product.categoryName}"/></div>
            <h1><c:out value="${product.name}"/></h1>
            <div class="rating product-rating" aria-label="${product.rating} trên 5 sao">
                <c:forEach begin="1" end="5" var="star">
                    <span class="${star <= product.rating ? 'star-on' : 'star-off'}">★</span>
                </c:forEach>
                <span class="muted"><c:out value="${product.rating}"/>/5 · <c:out value="${product.reviewCount}"/> đánh giá</span>
                <span class="muted">·</span>
                <a href="#comments" style="font-size:13px; color:var(--violet); text-decoration:underline;">
                    💬 Hỏi đáp (<c:out value="${commentsCount}"/>)
                </a>
            </div>
            <p class="description"><c:out value="${product.description}"/></p>
            <div class="detail-price">
                <c:choose>
                    <c:when test="${product.available}"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</c:when>
                    <c:otherwise><span class="unavailable-price">Tạm chưa có giá bán</span></c:otherwise>
                </c:choose>
            </div>
            <ul class="spec-list">
                <li><span>Phương pháp in</span><b><c:out value="${product.available ? product.printMethod : '—'}"/></b></li>
                <li><span>Số lượng tối thiểu</span><b><c:out value="${product.available ? product.minimumQuantity : '—'}"/><c:if test="${product.available}"> sản phẩm</c:if></b></li>
                <li><span>Gian hàng</span><b><c:out value="${product.available ? product.storeName : '—'}"/></b></li>
                <li><span>Đơn vị tính</span><b><c:out value="${product.baseUnit}"/></b></li>
                <li><span>Thương hiệu</span><b><c:out value="${empty product.brand ? 'Không có' : product.brand}"/></b></li>
                <li><span>Tùy chỉnh thiết kế</span><b>${product.customizable ? 'Có hỗ trợ' : 'Không hỗ trợ'}</b></li>
            </ul>
            <form class="product-purchase-panel js-cart-form" method="post"
                  action="${pageContext.request.contextPath}/cart"
                  data-checkout-url="${pageContext.request.contextPath}/order">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="add">
                <c:choose>
                    <c:when test="${not product.available}">
                        <div class="purchase-unavailable">Sản phẩm hiện chưa có gian hàng còn hàng.</div>
                    </c:when>
                    <c:otherwise>
                        <input type="hidden" name="shopProductId" value="${product.primaryOffer.shopProductId}">
                        <label class="purchase-quantity">
                            <span>Số lượng</span>
                            <input class="quantity-input" type="number" name="quantity" min="${product.minimumQuantity}" value="${product.minimumQuantity}">
                        </label>
                        <div class="product-action-grid">
                            <button class="button-primary product-action-button" type="submit" data-cart-intent="cart">
                                <span class="button-label">Thêm vào giỏ hàng</span>
                            </button>
                            <button class="button-buy-now product-action-button" type="submit" data-cart-intent="checkout">
                                <span class="button-label">Đặt ngay</span>
                            </button>
                        </div>
                    </c:otherwise>
                </c:choose>
            </form>
            <div class="product-secondary-actions">
                <c:choose>
                    <c:when test="${empty sessionScope.currentUser}">
                        <a class="favorite-button" href="${pageContext.request.contextPath}/login?redirect=/product-detail?id=${product.id}">
                            <span class="favorite-icon">♡</span><span>Yêu thích</span>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <form id="favoriteForm" class="js-favorite-form" method="post" action="${pageContext.request.contextPath}/favorites">
                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                            <input type="hidden" name="action" value="${favorite ? 'remove' : 'add'}">
                            <input type="hidden" name="productId" value="${product.id}">
                            <input type="hidden" name="returnUrl" value="/product-detail?id=${product.id}">
                            <button class="favorite-button ${favorite ? 'is-favorite' : ''}" type="submit" aria-pressed="${favorite}">
                                <span class="favorite-icon">${favorite ? '♥' : '♡'}</span>
                                <span class="favorite-label">${favorite ? 'Bỏ yêu thích' : 'Yêu thích'}</span>
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>
                <c:if test="${product.customizable}">
                    <a class="custom-quote-link" href="${pageContext.request.contextPath}/quotes?productId=${product.id}">Yêu cầu tùy chỉnh / báo giá</a>
                </c:if>
            </div>
        </section>
    </div>

    <%-- Các chào bán (gian hàng) cho sản phẩm này --%>
    <c:if test="${not empty product.offers}">
        <section class="summary-panel offers-panel">
            <h2>Lựa chọn gian hàng (<c:out value="${fn:length(product.offers)}"/>)</h2>
            <p class="muted">Mỗi gian hàng có riêng giá, vật liệu và thời gian sản xuất. Chọn chào bán phù hợp để thêm vào giỏ.</p>
            <div class="cart-list">
                <c:forEach var="offer" items="${product.offers}">
                    <div class="cart-line offer-line">
                        <div>
                            <h3><c:out value="${offer.shopName}"/></h3>
                            <p><c:out value="${offer.printingMethod}"/> · <c:out value="${offer.material}"/> ·
                                từ <c:out value="${offer.minimumQuantity}"/> sản phẩm · <c:out value="${offer.productionDays}"/> ngày sản xuất</p>
                        </div>
                        <strong><fmt:formatNumber value="${offer.unitPrice}" type="number" groupingUsed="true"/>đ</strong>
                        <c:choose>
                            <c:when test="${offer.purchasable}">
                                <form class="js-cart-form offer-add-form" method="post" action="${pageContext.request.contextPath}/cart">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="add">
                                    <input type="hidden" name="shopProductId" value="${offer.shopProductId}">
                                    <input class="quantity-input" type="number" name="quantity" min="${offer.minimumQuantity}" value="${offer.minimumQuantity}" style="width:80px;">
                                    <button class="button-primary" type="submit" data-cart-intent="cart"><span class="button-label">Thêm</span></button>
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

    <%-- Đánh giá đã đăng của khách đã mua --%>
    <section class="summary-panel" id="reviews" style="margin-top:32px;">
        <h2>Đánh giá sản phẩm (<c:out value="${totalPublishedReviews}"/>)</h2>
        <c:choose>
            <c:when test="${empty reviews}">
                <div class="empty-state">Chưa có đánh giá nào cho sản phẩm này.</div>
            </c:when>
            <c:otherwise>
                <div class="cart-list">
                    <c:forEach var="review" items="${reviews}">
                        <div class="cart-line">
                            <div>
                                <h3><c:out value="${review.userDisplayName}"/>
                                    <span class="rating" aria-label="${review.rating} trên 5 sao">
                                        <c:out value="${review.stars}"/><span class="star-off"><c:out value="${review.emptyStars}"/></span>
                                    </span>
                                </h3>
                                <c:if test="${not empty review.createdAtText}">
                                    <small class="review-date"><c:out value="${review.createdAtText}"/></small>
                                </c:if>
                                <p><c:out value="${review.reviewText}"/></p>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
        <c:if test="${totalReviewPages > 1}">
            <nav class="pagination" aria-label="Phân trang đánh giá">
                <c:if test="${reviewPage > 1}">
                    <a class="button-secondary" href="${pageContext.request.contextPath}/product-detail?id=${product.id}&amp;reviewPage=${reviewPage - 1}#reviews">Trang trước</a>
                </c:if>
                <span>Trang <c:out value="${reviewPage}"/> / <c:out value="${totalReviewPages}"/></span>
                <c:if test="${reviewPage < totalReviewPages}">
                    <a class="button-secondary" href="${pageContext.request.contextPath}/product-detail?id=${product.id}&amp;reviewPage=${reviewPage + 1}#reviews">Trang sau</a>
                </c:if>
            </nav>
        </c:if>
    </section>

+                                <section class="summary-panel" id="comments" style="margin-top:20px; padding: 18px 20px;">
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

    <c:if test="${not empty relatedProducts}">
        <section class="related-products" aria-labelledby="related-title">
            <h2 id="related-title">Sản phẩm liên quan</h2>
            <div class="products-grid related-grid">
                <c:forEach var="related" items="${relatedProducts}">
                    <article class="product-card">
                        <a href="${pageContext.request.contextPath}/product-detail?id=${related.id}">
                            <div class="product-visual ${related.visualClass}">
                                <c:choose>
                                    <c:when test="${not empty related.imageUrl}"><img src="<c:out value='${related.imageUrl}'/>" alt="<c:out value='${related.name}'/>"></c:when>
                                    <c:otherwise><c:out value="${related.name}"/></c:otherwise>
                                </c:choose>
                            </div>
                        </a>
                        <div class="product-copy">
                            <h3><a href="${pageContext.request.contextPath}/product-detail?id=${related.id}"><c:out value="${related.name}"/></a></h3>
                            <div class="meta"><c:out value="${related.categoryName}"/></div>
                            <div class="product-price">
                                <c:choose>
                                    <c:when test="${related.available}"><fmt:formatNumber value="${related.price}" type="number" groupingUsed="true"/>đ</c:when>
                                    <c:otherwise>Chưa có giá</c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </section>
    </c:if>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/product-detail.js?v=20261026-1"></script>
</body>
</html>
