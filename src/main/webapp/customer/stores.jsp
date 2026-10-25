<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tìm kiếm gian hàng - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Nhà cung cấp</div>
            <h1>Tìm kiếm gian hàng</h1>
            <p>Chọn xưởng in theo chuyên môn, khu vực và đánh giá của khách hàng.</p>
        </div>
    </div>
    <form class="search-line" method="get" action="${pageContext.request.contextPath}/stores">
        <label class="visually-hidden" for="store-query">Tìm theo tên, chuyên môn hoặc khu vực</label>
        <input id="store-query" type="search" name="q" value="<c:out value='${query}'/>"
               placeholder="Tên gian hàng, chuyên môn, khu vực...">
        <button class="button-primary" type="submit">Tìm gian hàng</button>
        <c:if test="${not empty query}">
            <a class="button-secondary" href="${pageContext.request.contextPath}/stores">Xóa tìm kiếm</a>
        </c:if>
    </form>
    <c:choose>
        <c:when test="${empty stores}">
            <div class="empty-state">Không tìm thấy gian hàng phù hợp.</div>
        </c:when>
        <c:otherwise>
            <div class="stores-grid">
                <c:forEach var="store" items="${stores}">
                    <article class="store-card">
                        <div class="eyebrow-line"><c:out value="${store.location}"/></div>
                        <h2><c:out value="${store.name}"/></h2>
                        <p><c:out value="${store.description}"/></p>
                        <div class="store-meta">
                            <c:if test="${not empty store.addressLine}">
                                <div><span>Địa chỉ:</span> <b><c:out value="${store.addressLine}"/></b></div>
                            </c:if>
                            <div>★ <b><c:out value="${store.rating}"/></b> <span>đánh giá trung bình</span></div>
                            <div><b><c:out value="${store.completedOrders}"/></b> <span>đơn đã hoàn thành</span></div>
                            <div><span>Chuyên môn:</span> <b><c:out value="${store.specialty}"/></b></div>
                            <c:if test="${not empty store.capabilities}">
                                <div><span>Năng lực:</span> <b><c:out value="${store.capabilities}"/></b></div>
                            </c:if>
                            <c:if test="${not empty store.acceptedFileFormats}">
                                <div><span>Định dạng nhận:</span> <b><c:out value="${store.acceptedFileFormats}"/></b></div>
                            </c:if>
                            <c:if test="${not empty store.maxBuildDimensions}">
                                <div><span>Kích thước tối đa:</span> <b><c:out value="${store.maxBuildDimensions}"/></b></div>
                            </c:if>
                            <c:if test="${not empty store.estimatedLeadDays}">
                                <div><span>Thời gian dự kiến:</span> <b><c:out value="${store.estimatedLeadDays}"/> ngày</b></div>
                            </c:if>
                            <c:if test="${not empty store.operatingHours}">
                                <div><span>Thời gian hoạt động:</span> <b><c:out value="${store.operatingHours}"/></b></div>
                            </c:if>
                            <c:if test="${not empty store.serviceNotes}">
                                <div><span>Lưu ý dịch vụ:</span> <b><c:out value="${store.serviceNotes}"/></b></div>
                            </c:if>
                            <c:if test="${not empty store.contactPhone}">
                                <div><span>Liên hệ:</span> <b><c:out value="${store.contactPhone}"/></b></div>
                            </c:if>
                            <c:if test="${not empty store.contactEmail}">
                                <div><span>Email:</span> <b><c:out value="${store.contactEmail}"/></b></div>
                            </c:if>
                        </div>
                        <a class="button-secondary" href="${pageContext.request.contextPath}/products?storeId=${store.id}">Xem sản phẩm của shop</a>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
