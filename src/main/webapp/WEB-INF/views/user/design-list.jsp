<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Yêu cầu in 3D của tôi | PrintKraft</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261126-customer-sync-r1"></head><body>
<jsp:include page="/customer/header.jsp"/><main class="uv2-page"><div class="uv2-breadcrumb"><a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span><span>Yêu cầu in 3D</span></div>
<div class="uv2-page-heading"><div><p class="uv2-eyebrow">CUSTOM PRINT ACTIVITY</p><h1>Yêu cầu in 3D của tôi</h1><p>Theo dõi mô hình, báo giá, đặt cọc 30% qua ZaloPay Sandbox và tiến độ in tại xưởng.</p></div><a class="uv2-button is-primary" href="${pageContext.request.contextPath}/design">＋ Tạo yêu cầu in</a></div>
<c:if test="${param.created eq '1'}"><div class="uv2-alert is-success">Yêu cầu <b><c:out value="${param.code}"/></b> đã được gửi thành công.</div></c:if>
<c:if test="${param.updated eq '1'}"><div class="uv2-alert is-success">Đã cập nhật trạng thái yêu cầu in 3D.</div></c:if>
<c:if test="${not empty param.error}"><div class="uv2-alert is-error"><c:out value="${param.error}"/></div></c:if>
<div class="uv2-account-grid"><jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/><div class="uv2-content"><section class="uv2-panel">
<c:choose><c:when test="${empty requests}"><div class="uv2-empty"><b>Bạn chưa tải mô hình 3D</b><p>Tải mô hình lên để chọn xưởng, đặt trực tiếp hoặc yêu cầu báo giá.</p><a class="uv2-button is-primary" href="${pageContext.request.contextPath}/design">Tải mô hình đầu tiên</a></div></c:when><c:otherwise><div class="uv2-design-request-list">
<c:set var="csrfTokenValue" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>"/>
<c:forEach var="r" items="${requests}"><article class="uv2-design-request uv2-design-card">
    <div class="uv2-design-card-head">
        <div class="uv2-design-card-identity">
            <div class="uv2-design-file-icon" aria-hidden="true">3D</div>
            <div class="uv2-design-card-heading">
                <div class="uv2-design-request-title">
                    <b><c:out value="${r.requestCode}"/></b>
                    <span class="uv2-status is-<c:out value="${r.status}"/>"><c:out value="${r.getStatusLabel()}"/></span>
                </div>
                <p class="uv2-design-filename" title="<c:out value="${r.originalFilename}"/>"><c:out value="${r.originalFilename}"/></p>
            </div>
        </div>
        <div class="uv2-design-request-price">
            <c:choose>
                <c:when test="${not empty r.quotedAmount}">
                    <small>Báo giá chính thức</small>
                    <b><fmt:formatNumber value="${r.quotedAmount}" type="number" groupingUsed="true"/> đ</b>
                    <c:if test="${r.isDepositable()}">
                        <span class="uv2-design-deposit-hint">Cần cọc 30%: <strong><fmt:formatNumber value="${r.getRequiredDepositAmount()}" type="number" groupingUsed="true"/> đ</strong></span>
                    </c:if>
                </c:when>
                <c:when test="${not empty r.estimatedTotal}">
                    <small>Ước tính theo xưởng</small>
                    <b><fmt:formatNumber value="${r.estimatedTotal}" type="number" groupingUsed="true"/> đ</b>
                </c:when>
                <c:otherwise>
                    <small>Chờ xưởng kiểm tra</small>
                    <b>—</b>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="uv2-design-specs">
        <div class="uv2-design-spec-item"><span>Đơn vị</span><b><c:out value="${r.sourceUnit}"/></b></div>
        <div class="uv2-design-spec-item"><span>Kích thước (mm)</span><b><c:out value="${r.widthMm}"/> × <c:out value="${r.depthMm}"/> × <c:out value="${r.heightMm}"/></b></div>
        <div class="uv2-design-spec-item"><span>Vật liệu</span><b><c:out value="${r.material}"/></b></div>
        <div class="uv2-design-spec-item"><span>Công nghệ</span><b><c:out value="${r.printingMethod}"/></b></div>
        <div class="uv2-design-spec-item"><span>Số lượng</span><b><c:out value="${r.quantity}"/></b></div>
        <c:if test="${not empty r.shopName}">
            <div class="uv2-design-spec-item"><span>Xưởng in</span><b><c:out value="${r.shopName}"/></b></div>
        </c:if>
        <c:if test="${not empty r.productionDays}">
            <div class="uv2-design-spec-item"><span>Thời gian dự kiến</span><b><c:out value="${r.productionDays}"/> ngày</b></div>
        </c:if>
        <c:if test="${not empty r.quoteValidUntil}">
            <div class="uv2-design-spec-item"><span>Hiệu lực báo giá</span><b><fmt:formatDate value="${r.quoteValidUntil}" pattern="dd/MM/yyyy HH:mm"/></b></div>
        </c:if>
    </div>

    <c:if test="${r.status eq 'DEPOSITED' or r.status eq 'PRINTING' or r.status eq 'COMPLETED'}">
        <div class="uv2-design-payment-bar">
            <div><span>Đã đặt cọc (30%)</span><b><fmt:formatNumber value="${r.getRequiredDepositAmount()}" type="number" groupingUsed="true"/> đ</b></div>
            <div><span>Còn lại khi giao</span><b><fmt:formatNumber value="${r.getRemainingAmount()}" type="number" groupingUsed="true"/> đ</b></div>
            <div><span>Thanh toán lúc</span><b><c:out value="${r.getDepositPaidAtText()}"/></b></div>
        </div>
    </c:if>

    <div class="uv2-design-notices">
        <c:if test="${not empty r.sellerResponseNote}">
            <div class="uv2-design-note"><span>Ghi chú của xưởng:</span> <c:out value="${r.sellerResponseNote}"/></div>
        </c:if>
        <c:if test="${r.status eq 'DEPOSITED'}">
            <div class="uv2-design-progress is-deposited">Đã đặt cọc qua ZaloPay Sandbox. Chờ xưởng bắt đầu in.</div>
        </c:if>
        <c:if test="${r.status eq 'PRINTING'}">
            <div class="uv2-design-progress is-printing">Xưởng đang in từ <c:out value="${r.getProductionStartedAtText()}"/><c:if test="${not empty r.productionNote}"> · <c:out value="${r.productionNote}"/></c:if></div>
        </c:if>
        <c:if test="${r.status eq 'COMPLETED'}">
            <div class="uv2-design-progress is-completed">Đã hoàn thành in lúc <c:out value="${r.getCompletedAtText()}"/><c:if test="${not empty r.productionNote}"> · <c:out value="${r.productionNote}"/></c:if><c:if test="${not empty r.orderCode}"> · Đơn hàng: <b><c:out value="${r.orderCode}"/></b></c:if></div>
        </c:if>
        <c:if test="${not empty r.analysisMessage}">
            <div class="uv2-design-analysis"><c:out value="${r.analysisMessage}"/></div>
        </c:if>
    </div>

    <div class="uv2-design-card-foot">
        <a class="uv2-button is-light" href="${pageContext.request.contextPath}/my-designs/file?id=${r.designFileId}">Tải file</a>
        <c:if test="${not empty r.orderCode}">
            <a class="uv2-button is-primary" href="${pageContext.request.contextPath}/order-tracking?code=${r.orderCode}">Theo dõi giao hàng ↗</a>
            <a class="uv2-button is-light" href="${pageContext.request.contextPath}/order-detail?code=${r.orderCode}">Chi tiết đơn (<c:out value="${r.orderCode}"/>)</a>
        </c:if>
        <c:if test="${r.status eq 'READY_TO_ORDER'}">
            <a class="uv2-button is-primary" href="${pageContext.request.contextPath}/order?mode=customPrint&amp;requestId=${r.id}">Xác nhận đặt trực tiếp</a>
        </c:if>
        <c:if test="${r.isDepositable()}">
            <form method="post" action="${pageContext.request.contextPath}/my-designs">
                <input type="hidden" name="csrfToken" value="${csrfTokenValue}">
                <input type="hidden" name="action" value="payDeposit">
                <input type="hidden" name="requestId" value="${r.id}">
                <button class="uv2-button is-primary is-zalopay" type="submit">
                    Đặt cọc 30% ZaloPay (<fmt:formatNumber value="${r.getRequiredDepositAmount()}" type="number" groupingUsed="true"/>đ)
                </button>
            </form>
        </c:if>
        <c:if test="${r.status eq 'QUOTED'}">
            <form method="post" action="${pageContext.request.contextPath}/my-designs">
                <input type="hidden" name="csrfToken" value="${csrfTokenValue}">
                <input type="hidden" name="action" value="declineQuote">
                <input type="hidden" name="requestId" value="${r.id}">
                <button class="uv2-button is-light" type="submit">Từ chối báo giá</button>
            </form>
        </c:if>
        <c:if test="${r.status eq 'QUOTED' or r.status eq 'CUSTOMER_DECLINED' or r.status eq 'EXPIRED' or r.status eq 'CLOSED'}">
            <form method="post" action="${pageContext.request.contextPath}/my-designs">
                <input type="hidden" name="csrfToken" value="${csrfTokenValue}">
                <input type="hidden" name="action" value="newQuoteDraft">
                <input type="hidden" name="requestId" value="${r.id}">
                <button class="uv2-button is-light" type="submit">Yêu cầu báo giá từ xưởng khác</button>
            </form>
        </c:if>
    </div>
</article></c:forEach></div></c:otherwise></c:choose></section></div></div></main><jsp:include page="/customer/footer.jsp"/></body></html>
