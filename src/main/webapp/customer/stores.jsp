<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Tìm xưởng in và nhà cung cấp phù hợp theo chuyên môn, khu vực và đánh giá thực tế.">
    <title>Nhà cung cấp - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css?v=20261125-modern1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261125-modern1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customer-modern.css?v=20261125-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/supplier-directory.css?v=20261125-1">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page supplier-directory">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">MẠNG LƯỚI ĐỐI TÁC PRINTKRAFT</div>
            <h1>Tìm nhà cung cấp phù hợp</h1>
            <p>Khám phá xưởng in theo chuyên môn, khu vực, năng lực sản xuất và đánh giá từ khách hàng. Mở hồ sơ để xem thông tin và lựa chọn cách liên hệ phù hợp.</p>
            <span class="supplier-count">
                <span class="supplier-count-dot" aria-hidden="true"></span>
                <c:out value="${fn:length(stores)}"/> nhà cung cấp phù hợp
            </span>
        </div>
    </div>

    <form class="search-line supplier-search" method="get" action="${pageContext.request.contextPath}/stores">
        <label class="visually-hidden" for="store-query">Tìm theo tên, chuyên môn hoặc khu vực</label>
        <input class="supplier-search-input" id="store-query" type="search" name="q"
               value="<c:out value='${query}'/>"
               placeholder="Nhập tên xưởng, chuyên môn hoặc tỉnh/thành..."
               autocomplete="off">
        <button class="button-primary" type="submit">
            <span aria-hidden="true">⌕</span> Tìm nhà cung cấp
        </button>
        <c:if test="${not empty query}">
            <a class="button-secondary" href="${pageContext.request.contextPath}/stores">Xóa tìm kiếm</a>
        </c:if>
    </form>

    <div class="supplier-results-head">
        <strong>Danh sách nhà cung cấp</strong>
        <span>Ưu tiên theo điểm đánh giá và tên xưởng</span>
    </div>

    <c:choose>
        <c:when test="${empty stores}">
            <section class="supplier-empty">
                <div class="supplier-empty-icon" aria-hidden="true">⌕</div>
                <h2>Chưa tìm thấy nhà cung cấp phù hợp</h2>
                <p>Thử tìm bằng tên xưởng, chuyên môn hoặc khu vực khác.</p>
                <c:if test="${not empty query}">
                    <a class="button-secondary" href="${pageContext.request.contextPath}/stores">Xem tất cả nhà cung cấp</a>
                </c:if>
            </section>
        </c:when>
        <c:otherwise>
            <div class="stores-grid supplier-grid">
                <c:forEach var="store" items="${stores}">
                    <article class="store-card supplier-card">
                        <div class="supplier-card-top">
                            <div class="supplier-avatar" aria-hidden="true">
                                <c:out value="${fn:substring(store.name, 0, 1)}"/>
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
                                    <img src="<c:out value='${supplierLogoSrc}'/>"
                                         alt=""
                                         width="68" height="68"
                                         loading="lazy" decoding="async"
                                         onerror="this.style.display='none';">
                                </c:if>
                            </div>

                            <div class="supplier-card-title">
                                <c:choose>
                                    <c:when test="${store.verificationStatus eq 'VERIFIED'}">
                                        <span class="supplier-verified"><span aria-hidden="true">✓</span> Đã xác minh</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="supplier-status">Nhà cung cấp</span>
                                    </c:otherwise>
                                </c:choose>
                                <h2><a href="${pageContext.request.contextPath}/store-detail?id=${store.id}"><c:out value="${store.name}"/></a></h2>
                                <div class="supplier-location">
                                    <span class="supplier-location-icon" aria-hidden="true">⌖</span>
                                    <span><c:out value="${store.location}"/></span>
                                </div>
                            </div>
                        </div>

                        <p class="supplier-card-description">
                            <c:choose>
                                <c:when test="${not empty store.description}"><c:out value="${store.description}"/></c:when>
                                <c:otherwise>Nhà cung cấp chưa cập nhật phần giới thiệu. Mở hồ sơ để xem thông tin dịch vụ hiện có.</c:otherwise>
                            </c:choose>
                        </p>

                        <div class="supplier-specialty">
                            <c:if test="${not empty store.specialty}">
                                <span><c:out value="${store.specialty}"/></span>
                            </c:if>
                            <c:if test="${not empty store.capabilities}">
                                <span><c:out value="${store.capabilities}"/></span>
                            </c:if>
                            <c:if test="${not empty store.estimatedLeadDays}">
                                <span>Dự kiến <c:out value="${store.estimatedLeadDays}"/> ngày</span>
                            </c:if>
                        </div>

                        <div class="supplier-stats">
                            <div class="supplier-stat">
                                <strong><span class="supplier-star" aria-hidden="true">★</span><fmt:formatNumber value="${store.rating}" minFractionDigits="1" maxFractionDigits="1"/></strong>
                                <span>Điểm trung bình</span>
                            </div>
                            <div class="supplier-stat">
                                <strong><c:out value="${store.reviewCount}"/></strong>
                                <span>Đánh giá công khai</span>
                            </div>
                            <div class="supplier-stat">
                                <strong><c:out value="${store.completedOrders}"/></strong>
                                <span>Đơn hoàn thành</span>
                            </div>
                        </div>

                        <div class="supplier-card-actions">
                            <a href="${pageContext.request.contextPath}/store-detail?id=${store.id}">Xem chi tiết</a>
                            <a href="${pageContext.request.contextPath}/store-detail?id=${store.id}#reviews">Xem đánh giá</a>
                            <a href="${pageContext.request.contextPath}/store-detail?id=${store.id}#contact">Liên hệ</a>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
