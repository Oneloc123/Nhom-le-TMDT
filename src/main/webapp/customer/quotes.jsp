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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104-account-layout">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/customer-modern.css?v=20261125-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account-modals.css?v=20261126-center-chat">
</head>
<body class="customer-account-body">
<%@ include file="header.jsp" %>

<main class="uv2-page uv2-quotes-page">
    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp" />

        <div class="uv2-content">

            <%-- Heading --%>
            <div class="uv2-page-heading">
                <div>
                    <p class="uv2-eyebrow">DOANH NGHIỆP</p>
                    <h1>Yêu cầu báo giá số lượng lớn</h1>
                    <p>Gửi yêu cầu báo giá cho đơn hàng doanh nghiệp; xưởng in sẽ phản hồi kèm báo giá.</p>
                </div>
                <button type="button" class="account-modal-button is-primary" data-modal-open="#quoteCreateModal">＋ Tạo yêu cầu báo giá</button>
            </div>

            <%-- Flash --%>
            <c:if test="${not empty param.created}">
                <div class="uv2-alert is-success" role="status">
                    Đã gửi yêu cầu báo giá. Mã yêu cầu: <c:out value="${param.created}"/>.
                </div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="uv2-alert is-error"><c:out value="${error}"/></div>
            </c:if>

            <%-- Layout 2 cột: danh sách bên trái, form bên phải --%>
            <div class="checkout-layout uv2-quote-layout">
                <section class="summary-panel uv2-quote-list-panel">
                    <h2>Yêu cầu của tôi</h2><p class="uv2-muted">Chọn “Xem chi tiết” trên từng yêu cầu để xem thông tin đầy đủ và phản hồi của xưởng.</p>
                    <c:choose>
                        <c:when test="${empty quotes}">
                            <div class="empty-state">Bạn chưa gửi yêu cầu báo giá nào.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="uv2-quote-list">
                                <c:forEach var="q" items="${quotes}">
                                    <article class="uv2-quote-card">
                                        <div class="uv2-quote-main">
                                            <div class="uv2-quote-card-top">
                                                <span class="uv2-quote-code">MÃ YÊU CẦU #<c:out value="${q.quoteRequestId}"/></span>
                                                <span class="uv2-status"><c:out value="${q.status}"/></span>
                                            </div>
                                            <h3><c:out value="${q.productName}"/></h3>
                                            <p class="uv2-quote-company"><b>Doanh nghiệp:</b> <c:out value="${q.companyName}"/></p>
                                            <div class="uv2-quote-meta">
                                                <span><b>Số lượng:</b> <c:out value="${q.requestedQuantity}"/> sản phẩm</span>
                                                <span><b>Cần trước:</b> <c:out value="${q.requiredByText}"/></span>
                                                <c:if test="${not empty q.preferredShopName}"><span><b>Xưởng ưu tiên:</b> <c:out value="${q.preferredShopName}"/></span></c:if>
                                            </div>
                                        </div>
                                        <div class="uv2-quote-summary">
                                            <c:if test="${not empty q.quotedAmount}">
                                                <span class="uv2-quote-price-label">Báo giá hiện tại</span>
                                                <strong class="uv2-quote-price"><fmt:formatNumber value="${q.quotedAmount}" type="number" groupingUsed="true"/>đ</strong>
                                            </c:if>
                                            <button type="button" class="account-modal-button is-primary" data-modal-open="#quoteDetailModal${q.quoteRequestId}">Xem chi tiết <span aria-hidden="true">↗</span></button>
                                        </div>
                                    </article>
                                    <dialog id="quoteDetailModal${q.quoteRequestId}" class="account-modal account-modal--detail" data-account-modal aria-labelledby="quoteDetailHeading${q.quoteRequestId}">
                                        <div class="account-modal-panel">
                                            <div class="account-modal-header">
                                                <div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">₫</span><div><h2 id="quoteDetailHeading${q.quoteRequestId}">Chi tiết yêu cầu #<c:out value="${q.quoteRequestId}"/></h2><p>Thông tin yêu cầu và phản hồi báo giá từ xưởng.</p></div></div>
                                                <button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button>
                                            </div>
                                            <div class="account-modal-scroll">
                                                <div class="account-modal-detail-grid">
                                                    <div class="account-modal-detail-item"><span>Trạng thái</span><strong><c:out value="${q.status}"/></strong></div>
                                                    <div class="account-modal-detail-item"><span>Sản phẩm</span><strong><c:out value="${q.productName}"/></strong></div>
                                                    <div class="account-modal-detail-item"><span>Doanh nghiệp</span><strong><c:out value="${q.companyName}"/></strong></div>
                                                    <div class="account-modal-detail-item"><span>Số lượng</span><strong><c:out value="${q.requestedQuantity}"/> sản phẩm</strong></div>
                                                    <div class="account-modal-detail-item"><span>Ngày cần nhận</span><strong><c:out value="${q.requiredByText}"/></strong></div>
                                                    <div class="account-modal-detail-item"><span>Xưởng ưu tiên</span><strong><c:choose><c:when test="${not empty q.preferredShopName}"><c:out value="${q.preferredShopName}"/></c:when><c:otherwise>Không chọn</c:otherwise></c:choose></strong></div>
                                                    <div class="account-modal-detail-item"><span>Giá xưởng báo</span><strong><c:choose><c:when test="${not empty q.quotedAmount}"><fmt:formatNumber value="${q.quotedAmount}" type="number" groupingUsed="true"/>đ</c:when><c:otherwise>Chưa có báo giá</c:otherwise></c:choose></strong></div>
                                                </div>
                                                <c:if test="${not empty q.sellerResponseNote}">
                                                    <section class="account-modal-response">
                                                        <p class="account-modal-response-label">Phản hồi từ xưởng</p>
                                                        <p><c:out value="${q.sellerResponseNote}"/></p>
                                                    </section>
                                                </c:if>
                                            </div>
                                            <div class="account-modal-footer"><span class="account-modal-footer-note">Mã yêu cầu #<c:out value="${q.quoteRequestId}"/></span><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-primary" data-modal-close>Đóng chi tiết</button></div></div>
                                        </div>
                                    </dialog>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <dialog id="quoteCreateModal" class="account-modal" data-account-modal data-modal-auto-open="${not empty error ? 'true' : 'false'}" aria-labelledby="quoteCreateHeading">
    <div class="account-modal-panel">
        <div class="account-modal-header"><div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">₫</span><div><h2 id="quoteCreateHeading">Gửi yêu cầu báo giá</h2><p>Cung cấp đủ thông tin để xưởng đánh giá số lượng và thời gian thực hiện.</p></div></div><button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button></div>
        <div class="account-modal-scroll">
            <form id="quoteCreateForm" class="stack-form account-modal-form" method="post" action="${pageContext.request.contextPath}/quotes">
                        <input type="hidden" name="csrfToken"
                               value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">

                        <div class="field">
                            <label for="companyName">Tên doanh nghiệp</label>
                            <input id="companyName" type="text" name="companyName" maxlength="180" required value="<c:out value='${param.companyName}'/>">
                        </div>

                        <div class="field">
                            <label for="productId">Sản phẩm cần in</label>
                            <select id="productId" name="productId" required>
                                <option value="">— Chọn sản phẩm —</option>
                                <c:forEach var="p" items="${products}">
                                    <option value="${p.id}"
                                        <c:if test="${p.id == selectedProductId or p.id == param.productId}">selected</c:if>>
                                        <c:out value="${p.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="field">
                            <label for="preferredShopId">Gian hàng ưu tiên (không bắt buộc)</label>
                            <select id="preferredShopId" name="preferredShopId">
                                <option value="">— Không cụ thể —</option>
                                <c:forEach var="s" items="${shops}">
                                    <option value="${s.id}" <c:if test="${s.id == param.preferredShopId}">selected</c:if>><c:out value="${s.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="field">
                            <label for="quantity">Số lượng yêu cầu</label>
                            <input id="quantity" type="number" name="quantity" min="1" max="1000000" required value="<c:out value='${param.quantity}'/>">
                        </div>

                        <div class="field">
                            <label for="requiredBy">Cần nhận trước (tùy chọn)</label>
                            <input id="requiredBy" type="date" name="requiredBy" value="<c:out value='${param.requiredBy}'/>">
                        </div>

                        <p class="account-modal-form-note span-full">Sau khi gửi, bạn có thể theo dõi trạng thái và phản hồi của xưởng tại mục “Yêu cầu của tôi”.</p>
                    </form>
        </div>
        <div class="account-modal-footer"><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-light" data-modal-close>Hủy</button><button type="submit" form="quoteCreateForm" class="account-modal-button is-primary">Gửi yêu cầu báo giá</button></div></div>
    </div>
</dialog>
            </div>

        </div>
    </div>
</main>

<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/account-modals.js?v=20261125-modals"></script>
</body>
</html>