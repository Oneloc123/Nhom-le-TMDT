<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<!-- PrintKraft supplier-detail view version: 2026-11-25-v2 -->
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Xem hồ sơ, năng lực dịch vụ, thông tin liên hệ và đánh giá công khai của nhà cung cấp PrintKraft.">
    <title><c:out value="${store.name}"/> - Nhà cung cấp PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css?v=20261125-modern1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261125-modern1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customer-modern.css?v=20261125-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/supplier-directory.css?v=20261125-1">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="supplier-detail-page" data-view-version="2026-11-25-v2">
    <nav class="supplier-breadcrumb" aria-label="Đường dẫn">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <span aria-hidden="true">/</span>
        <a href="${pageContext.request.contextPath}/stores">Nhà cung cấp</a>
        <span aria-hidden="true">/</span>
        <span><c:out value="${store.name}"/></span>
    </nav>

    <section class="supplier-profile-hero">
        <div class="supplier-profile-top">
            <div class="supplier-avatar supplier-profile-avatar" aria-hidden="true">
                <c:choose><c:when test="${not empty store.name}"><c:out value="${fn:substring(store.name, 0, 1)}"/></c:when><c:otherwise>P</c:otherwise></c:choose>
                <c:if test="${not empty store.logoUrl}">
                    <c:choose>
                        <c:when test="${fn:startsWith(store.logoUrl, 'http://') or fn:startsWith(store.logoUrl, 'https://') or fn:startsWith(store.logoUrl, '//')}">
                            <c:set var="supplierLogoSrc" value="${store.logoUrl}"/>
                        </c:when>
                        <c:when test="${fn:startsWith(store.logoUrl, '/')}">
                            <c:set var="supplierLogoSrc" value="${pageContext.request.contextPath}${store.logoUrl}"/>
                        </c:when>
                        <c:otherwise>
                            <c:url var="supplierLogoSrc" value="/${store.logoUrl}"/>
                        </c:otherwise>
                    </c:choose>
                    <img src="<c:out value='${supplierLogoSrc}'/>" alt=""
                         width="100" height="100" decoding="async"
                         onerror="this.style.display='none';">
                </c:if>
            </div>

            <div class="supplier-profile-title">
                <c:choose>
                    <c:when test="${store.verificationStatus eq 'VERIFIED'}">
                        <span class="supplier-verified"><span aria-hidden="true">✓</span> Nhà cung cấp đã xác minh</span>
                    </c:when>
                    <c:otherwise>
                        <span class="supplier-status">Hồ sơ nhà cung cấp</span>
                    </c:otherwise>
                </c:choose>
                <h1><c:out value="${store.name}"/></h1>
                <div class="supplier-location">
                    <span class="supplier-location-icon" aria-hidden="true">⌖</span>
                    <span><c:out value="${store.location}"/></span>
                </div>
                <p class="supplier-lead">
                    <c:choose>
                        <c:when test="${not empty store.description}"><c:out value="${store.description}"/></c:when>
                        <c:otherwise>Nhà cung cấp chưa cập nhật phần giới thiệu.</c:otherwise>
                    </c:choose>
                </p>
                <div class="supplier-profile-actions">
                    <a href="${pageContext.request.contextPath}/products?storeId=${store.id}">Xem sản phẩm của xưởng</a>
                    <a href="#reviews">Xem đánh giá</a>
                    <a href="#contact">Liên hệ nhà cung cấp</a>
                </div>
            </div>
        </div>

        <div class="supplier-overview-stats">
            <article>
                <span>Đánh giá từ khách hàng</span>
                <strong><fmt:formatNumber value="${ratingSummary.averageRating}" minFractionDigits="1" maxFractionDigits="1"/> <em>★</em></strong>
            </article>
            <article>
                <span>Đánh giá đã công khai</span>
                <strong><c:out value="${reviewCount}"/></strong>
            </article>
            <article>
                <span>Đơn hàng hoàn thành</span>
                <strong><c:out value="${store.completedOrders}"/></strong>
            </article>
        </div>
    </section>

    <div class="supplier-detail-grid">
        <div class="supplier-detail-main">
            <section class="supplier-detail-panel" aria-labelledby="supplier-info-title">
                <div class="supplier-detail-panel-head">
                    <div>
                        <p class="supplier-eyebrow">HỒ SƠ DỊCH VỤ</p>
                        <h2 id="supplier-info-title">Thông tin nhà cung cấp</h2>
                    </div>
                    <span class="supplier-status"><c:out value="${store.specialty}"/></span>
                </div>

                <dl class="supplier-info-list">
                    <c:if test="${not empty store.addressLine}">
                        <div class="supplier-info-row"><dt>Địa chỉ</dt><dd><c:out value="${store.addressLine}"/></dd></div>
                    </c:if>
                    <div class="supplier-info-row"><dt>Khu vực phục vụ</dt><dd><c:out value="${store.location}"/></dd></div>
                    <c:if test="${not empty store.capabilities}">
                        <div class="supplier-info-row"><dt>Năng lực sản xuất</dt><dd><c:out value="${store.capabilities}"/></dd></div>
                    </c:if>
                    <c:if test="${not empty store.acceptedFileFormats}">
                        <div class="supplier-info-row"><dt>Định dạng tệp nhận</dt><dd><c:out value="${store.acceptedFileFormats}"/></dd></div>
                    </c:if>
                    <c:if test="${not empty store.maxBuildDimensions}">
                        <div class="supplier-info-row"><dt>Kích thước tối đa</dt><dd><c:out value="${store.maxBuildDimensions}"/></dd></div>
                    </c:if>
                    <c:if test="${not empty store.estimatedLeadDays}">
                        <div class="supplier-info-row"><dt>Thời gian dự kiến</dt><dd><c:out value="${store.estimatedLeadDays}"/> ngày</dd></div>
                    </c:if>
                    <c:if test="${not empty store.operatingHours}">
                        <div class="supplier-info-row"><dt>Thời gian hoạt động</dt><dd><c:out value="${store.operatingHours}"/></dd></div>
                    </c:if>
                    <c:if test="${not empty store.serviceNotes}">
                        <div class="supplier-info-row"><dt>Lưu ý dịch vụ</dt><dd><c:out value="${store.serviceNotes}"/></dd></div>
                    </c:if>
                </dl>
            </section>

            <section class="supplier-detail-panel" id="reviews" aria-labelledby="supplier-reviews-title">
                <div class="supplier-detail-panel-head">
                    <div>
                        <p class="supplier-eyebrow">TRẢI NGHIỆM KHÁCH HÀNG</p>
                        <h2 id="supplier-reviews-title">Đánh giá nhà cung cấp</h2>
                    </div>
                    <span class="supplier-status"><c:out value="${reviewCount}"/> đánh giá</span>
                </div>

                <div class="supplier-review-summary">
                    <div class="supplier-review-score">
                        <strong><fmt:formatNumber value="${ratingSummary.averageRating}" minFractionDigits="1" maxFractionDigits="1"/></strong>
                        <span class="supplier-stars" aria-label="Điểm đánh giá trung bình trên 5 sao"><c:forEach begin="1" end="5" var="summaryStar"><span class="${summaryStar le ratingSummary.averageRating ? '' : 'is-empty'}">★</span></c:forEach></span>
                        <small><c:out value="${ratingSummary.totalReviews}"/> đánh giá công khai</small>
                    </div>
                    <div class="supplier-rating-bars" aria-label="Phân bố số sao">
                        <div class="supplier-rating-row">
                            <span>5 sao</span><div class="supplier-rating-track"><div class="supplier-rating-fill" style="width:<c:out value='${ratingSummary.percent5}'/>%;"></div></div><strong><c:out value="${ratingSummary.star5Count}"/></strong>
                        </div>
                        <div class="supplier-rating-row">
                            <span>4 sao</span><div class="supplier-rating-track"><div class="supplier-rating-fill" style="width:<c:out value='${ratingSummary.percent4}'/>%;"></div></div><strong><c:out value="${ratingSummary.star4Count}"/></strong>
                        </div>
                        <div class="supplier-rating-row">
                            <span>3 sao</span><div class="supplier-rating-track"><div class="supplier-rating-fill" style="width:<c:out value='${ratingSummary.percent3}'/>%;"></div></div><strong><c:out value="${ratingSummary.star3Count}"/></strong>
                        </div>
                        <div class="supplier-rating-row">
                            <span>2 sao</span><div class="supplier-rating-track"><div class="supplier-rating-fill" style="width:<c:out value='${ratingSummary.percent2}'/>%;"></div></div><strong><c:out value="${ratingSummary.star2Count}"/></strong>
                        </div>
                        <div class="supplier-rating-row">
                            <span>1 sao</span><div class="supplier-rating-track"><div class="supplier-rating-fill" style="width:<c:out value='${ratingSummary.percent1}'/>%;"></div></div><strong><c:out value="${ratingSummary.star1Count}"/></strong>
                        </div>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${empty reviews}">
                        <div class="supplier-review-empty">
                            <strong>Chưa có đánh giá công khai</strong>
                            <p>Khi có khách hàng gửi đánh giá và đánh giá được công khai, nội dung sẽ xuất hiện tại đây.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="supplier-review-list">
                            <c:forEach var="review" items="${reviews}">
                                <article class="supplier-review-card">
                                    <div class="supplier-review-header">
                                        <div class="supplier-review-avatar" aria-hidden="true"><c:out value="${review.userInitial}"/></div>
                                        <div class="supplier-review-author">
                                            <strong><c:out value="${review.userDisplayName}"/></strong>
                                            <c:if test="${not empty review.createdAtText}">
                                                <small><c:out value="${review.createdAtText}"/></small>
                                            </c:if>
                                            <div class="supplier-review-badges">
                                                <c:if test="${review.verifiedBuyer}"><span>✓ Đánh giá gắn với đơn hàng</span></c:if>
                                                <span><c:out value="${review.ratingLabel}"/></span>
                                            </div>
                                        </div>
                                        <span class="supplier-review-stars" aria-label="${review.rating} trên 5 sao">
                                            <c:out value="${review.stars}"/><span class="is-empty"><c:out value="${review.emptyStars}"/></span>
                                        </span>
                                    </div>
                                    <p class="supplier-review-text"><c:out value="${review.reviewText}"/></p>
                                    <c:if test="${not empty review.productName}">
                                        <div class="supplier-review-product">
                                            Sản phẩm:
                                            <c:choose>
                                                <c:when test="${not empty review.productId}">
                                                    <a href="${pageContext.request.contextPath}/product-detail?id=${review.productId}"><c:out value="${review.productName}"/></a>
                                                </c:when>
                                                <c:otherwise><c:out value="${review.productName}"/></c:otherwise>
                                            </c:choose>
                                        </div>
                                    </c:if>
                                </article>
                            </c:forEach>
                        </div>

                        <c:if test="${totalPages gt 1}">
                            <nav class="supplier-pagination" aria-label="Phân trang đánh giá">
                                <c:if test="${currentPage gt 1}">
                                    <a href="${pageContext.request.contextPath}/store-detail?id=${store.id}&amp;page=${currentPage - 1}#reviews">← Trang trước</a>
                                </c:if>
                                <span class="is-current">Trang <c:out value="${currentPage}"/> / <c:out value="${totalPages}"/></span>
                                <c:if test="${currentPage lt totalPages}">
                                    <a href="${pageContext.request.contextPath}/store-detail?id=${store.id}&amp;page=${currentPage + 1}#reviews">Trang sau →</a>
                                </c:if>
                            </nav>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <aside class="supplier-detail-aside">
            <section class="supplier-detail-panel supplier-contact-card" id="contact" aria-labelledby="supplier-contact-title">
                <div class="supplier-detail-panel-head">
                    <div>
                        <p class="supplier-eyebrow">KẾT NỐI TRỰC TIẾP</p>
                        <h2 id="supplier-contact-title">Liên hệ nhà cung cấp</h2>
                    </div>
                </div>
                <p class="supplier-contact-note">Liên hệ qua thông tin nhà cung cấp đã công khai để trao đổi về nhu cầu, số lượng, vật liệu và thời gian thực hiện.</p>

                <div class="supplier-contact-actions">
                    <c:if test="${not empty store.contactPhone}">
                        <c:set var="supplierPhoneLink" value="${fn:replace(store.contactPhone, ' ', '')}"/>
                        <a href="tel:<c:out value='${supplierPhoneLink}'/>">
                            <span class="supplier-contact-icon" aria-hidden="true">☎</span>
                            <span><strong>Gọi điện</strong><small><c:out value="${store.contactPhone}"/></small></span>
                        </a>
                    </c:if>
                    <c:if test="${not empty store.contactEmail}">
                        <a href="mailto:<c:out value='${store.contactEmail}'/>">
                            <span class="supplier-contact-icon" aria-hidden="true">✉</span>
                            <span><strong>Gửi email</strong><small><c:out value="${store.contactEmail}"/></small></span>
                        </a>
                    </c:if>
                    <a href="${pageContext.request.contextPath}/products?storeId=${store.id}">
                        <span class="supplier-contact-icon" aria-hidden="true">⌂</span>
                        <span><strong>Xem sản phẩm</strong><small>Khám phá sản phẩm và chào bán của xưởng</small></span>
                    </a>
                </div>

                <c:if test="${empty store.contactPhone and empty store.contactEmail}">
                    <p class="supplier-contact-note">Nhà cung cấp chưa công khai số điện thoại hoặc email. Bạn có thể xem sản phẩm và đặt hàng; hộp thư nội bộ hiện hỗ trợ trao đổi theo đơn hàng.</p>
                </c:if>
            </section>

            <section class="supplier-detail-panel">
                <div class="supplier-detail-panel-head">
                    <div>
                        <p class="supplier-eyebrow">BẮT ĐẦU MUA HÀNG</p>
                        <h2>Đặt sản phẩm từ xưởng</h2>
                    </div>
                </div>
                <p class="supplier-contact-note">Xem các sản phẩm đang được chào bán, sau đó chọn mẫu phù hợp để kiểm tra giá, số lượng tối thiểu và thông tin đặt hàng.</p>
                <div class="supplier-profile-actions">
                    <a href="${pageContext.request.contextPath}/products?storeId=${store.id}">Xem sản phẩm</a>
                </div>
            </section>
        </aside>
    </div>
</main>

<%@ include file="footer.jsp" %>
</body>
</html>
