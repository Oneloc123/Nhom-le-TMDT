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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account-modals.css?v=20261125-modals">
</head>
<body class="customer-account-body">
<%@ include file="header.jsp" %>

<main class="uv2-page">
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
            <div class="checkout-layout">
                <section class="summary-panel">
                    <h2>Yêu cầu của tôi</h2>
                    <c:choose>
                        <c:when test="${empty quotes}">
                            <div class="empty-state">Bạn chưa gửi yêu cầu báo giá nào.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="cart-list">
                                <c:forEach var="q" items="${quotes}">
                                    <div class="cart-line">
                                        <div>
                                            <h3>#<c:out value="${q.quoteRequestId}"/> — <c:out value="${q.productName}"/></h3>
                                            <p>
                                                <c:out value="${q.companyName}"/> ·
                                                <c:out value="${q.requestedQuantity}"/> sản phẩm ·
                                                cần trước <c:out value="${q.requiredByText}"/>
                                                <c:if test="${not empty q.preferredShopName}">
                                                    · Ưu tiên: <c:out value="${q.preferredShopName}"/>
                                                </c:if>
                                            </p>
                                        </div>
                                        <div>
                                            <b><c:out value="${q.status}"/></b>
                                            <c:if test="${not empty q.quotedAmount}">
                                                <div>
                                                    <fmt:formatNumber value="${q.quotedAmount}" type="number" groupingUsed="true"/>đ
                                                </div>
                                            </c:if>
                                            <c:if test="${not empty q.sellerResponseNote}">
                                                <div class="muted"><c:out value="${q.sellerResponseNote}"/></div>
                                            </c:if>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <aside class="order-panel account-modal-launch"><div><strong>Yêu cầu cần số lượng lớn?</strong><p>Gửi thông tin sản phẩm và số lượng dự kiến để xưởng tiếp nhận và phản hồi báo giá.</p></div><button type="button" class="account-modal-button is-primary" data-modal-open="#quoteCreateModal">Tạo yêu cầu mới</button></aside>
<dialog id="quoteCreateModal" class="account-modal" data-account-modal data-modal-auto-open="${not empty error ? 'true' : 'false'}" aria-labelledby="quoteCreateHeading">
    <div class="account-modal-panel">
        <div class="account-modal-header"><div class="account-modal-heading"><span class="account-modal-icon" aria-hidden="true">₫</span><div><h2 id="quoteCreateHeading">Gửi yêu cầu báo giá</h2><p>Cung cấp đủ thông tin để xưởng đánh giá số lượng và thời gian thực hiện.</p></div></div><button type="button" class="account-modal-close" data-modal-close aria-label="Đóng">×</button></div>
        <div class="account-modal-scroll">
            <aside class="account-modal-help" id="quoteCreateHelp" data-modal-help><div class="account-modal-help-title"><span aria-hidden="true">ⓘ</span> Hướng dẫn điền yêu cầu</div><ol><li>Chọn đúng sản phẩm bạn cần đặt số lượng lớn.</li><li>Nhập số lượng dự kiến; số lượng càng chính xác thì báo giá càng sát nhu cầu.</li><li>Có thể chọn xưởng ưu tiên, nhưng đây là trường không bắt buộc.</li><li>Chọn ngày cần nhận nếu đã có kế hoạch. Sau khi gửi, theo dõi trạng thái ở danh sách yêu cầu bên dưới.</li></ol><p>Gửi báo giá không đồng nghĩa với việc đơn hàng đã được đặt.</p></aside>
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
                                        <c:if test="${p.id == selectedProductId}">selected</c:if>>
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
                                    <option value="${s.id}"><c:out value="${s.name}"/></option>
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
        <div class="account-modal-footer"><button type="button" class="account-modal-help-toggle" data-help-toggle aria-controls="quoteCreateHelp" aria-expanded="true">Ẩn hướng dẫn</button><div class="account-modal-footer-main"><button type="button" class="account-modal-button is-light" data-modal-close>Hủy</button><button type="submit" form="quoteCreateForm" class="account-modal-button is-primary">Gửi yêu cầu báo giá</button></div></div>
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