<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Yêu cầu in 3D của tôi | PrintKraft</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104-account-layout"></head><body>
<jsp:include page="/customer/header.jsp"/><main class="uv2-page"><div class="uv2-breadcrumb"><a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span><span>Yêu cầu in 3D</span></div>
<div class="uv2-page-heading"><div><p class="uv2-eyebrow">CUSTOM PRINT ACTIVITY</p><h1>Yêu cầu in 3D của tôi</h1><p>Theo dõi mô hình, báo giá, đặt cọc 30% qua ZaloPay Sandbox và tiến độ in tại xưởng.</p></div><a class="uv2-button is-primary" href="${pageContext.request.contextPath}/design">＋ Tạo yêu cầu in</a></div>
<c:if test="${param.created eq '1'}"><div class="uv2-alert is-success">Yêu cầu <b><c:out value="${param.code}"/></b> đã được gửi thành công.</div></c:if>
<c:if test="${param.updated eq '1'}"><div class="uv2-alert is-success">Đã cập nhật trạng thái yêu cầu in 3D.</div></c:if>
<c:if test="${not empty param.error}"><div class="uv2-alert is-error"><c:out value="${param.error}"/></div></c:if>
<div class="uv2-account-grid"><jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/><div class="uv2-content"><section class="uv2-panel">
<c:choose><c:when test="${empty requests}"><div class="uv2-empty"><b>Bạn chưa tải mô hình 3D</b><p>Tải mô hình lên để chọn xưởng, đặt trực tiếp hoặc yêu cầu báo giá.</p><a class="uv2-button is-primary" href="${pageContext.request.contextPath}/design">Tải mô hình đầu tiên</a></div></c:when><c:otherwise><div class="uv2-design-request-list">
<c:set var="csrfTokenValue" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>"/>
<c:forEach var="r" items="${requests}"><article class="uv2-design-request"><div class="uv2-design-file-icon">3D</div><div class="uv2-design-request-main"><div class="uv2-design-request-title"><b><c:out value="${r.requestCode}"/></b><span class="uv2-status"><c:out value="${r.getStatusLabel()}"/></span></div><p><c:out value="${r.originalFilename}"/></p>
<div class="uv2-order-meta"><span>Đơn vị: <b><c:out value="${r.sourceUnit}"/></b></span><span>Kích thước mm: <b><c:out value="${r.widthMm}"/> × <c:out value="${r.depthMm}"/> × <c:out value="${r.heightMm}"/></b></span><span>Vật liệu: <b><c:out value="${r.material}"/></b></span><span>Công nghệ: <b><c:out value="${r.printingMethod}"/></b></span><span>Số lượng: <b><c:out value="${r.quantity}"/></b></span></div>
<c:if test="${not empty r.shopName}"><small>Xưởng: <b><c:out value="${r.shopName}"/></b></small></c:if>
<c:if test="${not empty r.productionDays or not empty r.quoteValidUntil}"><div class="uv2-order-meta"><c:if test="${not empty r.productionDays}"><span>Thời gian dự kiến: <b><c:out value="${r.productionDays}"/> ngày</b></span></c:if><c:if test="${not empty r.quoteValidUntil}"><span>Báo giá có hiệu lực đến: <b><fmt:formatDate value="${r.quoteValidUntil}" pattern="dd/MM/yyyy HH:mm"/></b></span></c:if></div></c:if>
<c:if test="${not empty r.sellerResponseNote}"><small>Ghi chú của xưởng: <c:out value="${r.sellerResponseNote}"/></small></c:if>
<c:if test="${r.status eq 'DEPOSITED' or r.status eq 'PRINTING' or r.status eq 'COMPLETED'}">
    <div class="uv2-order-meta">
        <span>Đã đặt cọc (30%): <b><fmt:formatNumber value="${r.getRequiredDepositAmount()}" type="number" groupingUsed="true"/> đ</b></span>
        <span>Còn lại khi giao: <b><fmt:formatNumber value="${r.getRemainingAmount()}" type="number" groupingUsed="true"/> đ</b></span>
        <span>Thanh toán lúc: <b><c:out value="${r.getDepositPaidAtText()}"/></b></span>
    </div>
</c:if>
<c:if test="${r.status eq 'DEPOSITED'}"><small style="color:#147a52;font-weight:600;">Đã đặt cọc qua ZaloPay Sandbox. Chờ xưởng bắt đầu in.</small></c:if>
<c:if test="${r.status eq 'PRINTING'}"><small style="color:#a86209;font-weight:600;">Xưởng đang in từ <c:out value="${r.getProductionStartedAtText()}"/><c:if test="${not empty r.productionNote}"> · <c:out value="${r.productionNote}"/></c:if></small></c:if>
<c:if test="${r.status eq 'COMPLETED'}"><small style="color:#166534;font-weight:600;">Đã hoàn thành in lúc <c:out value="${r.getCompletedAtText()}"/><c:if test="${not empty r.productionNote}"> · <c:out value="${r.productionNote}"/></c:if><c:if test="${not empty r.orderCode}"> · Đơn hàng: <b><c:out value="${r.orderCode}"/></b></c:if></small></c:if>
<c:if test="${not empty r.analysisMessage}"><small><c:out value="${r.analysisMessage}"/></small></c:if><small>Mã yêu cầu: <c:out value="${r.requestCode}"/></small></div>
<div class="uv2-design-request-price"><c:choose><c:when test="${not empty r.quotedAmount}"><small>Báo giá chính thức</small><b><fmt:formatNumber value="${r.quotedAmount}" type="number" groupingUsed="true"/> đ</b><c:if test="${r.isDepositable()}"><small style="margin-top:6px;">Cần cọc 30%: <b style="display:inline;font-size:11px;color:#0068ff;"><fmt:formatNumber value="${r.getRequiredDepositAmount()}" type="number" groupingUsed="true"/> đ</b></small></c:if></c:when><c:when test="${not empty r.estimatedTotal}"><small>Ước tính theo xưởng</small><b><fmt:formatNumber value="${r.estimatedTotal}" type="number" groupingUsed="true"/> đ</b></c:when><c:otherwise><small>Chờ xưởng kiểm tra</small><b>—</b></c:otherwise></c:choose></div>
<div class="uv2-custom-actions"><a class="uv2-button is-light" href="${pageContext.request.contextPath}/my-designs/file?id=${r.designFileId}">Tải file</a>
<c:if test="${not empty r.orderCode}">
    <a class="uv2-button is-primary" href="${pageContext.request.contextPath}/order-tracking?code=${r.orderCode}">Theo dõi giao hàng ↗</a>
    <a class="uv2-button is-light" href="${pageContext.request.contextPath}/order-detail?code=${r.orderCode}">Chi tiết đơn (<c:out value="${r.orderCode}"/>)</a>
</c:if>
<c:if test="${r.status eq 'READY_TO_ORDER'}"><a class="uv2-button is-primary" href="${pageContext.request.contextPath}/order?mode=customPrint&amp;requestId=${r.id}">Xác nhận đặt trực tiếp</a></c:if>
<c:if test="${r.isDepositable()}">
    <form method="post" action="${pageContext.request.contextPath}/my-designs">
        <input type="hidden" name="csrfToken" value="${csrfTokenValue}">
        <input type="hidden" name="action" value="payDeposit">
        <input type="hidden" name="requestId" value="${r.id}">
        <button class="uv2-button is-primary" style="background:#0068ff;border-color:#0068ff;" type="submit">
            Đặt cọc 30% ZaloPay (<fmt:formatNumber value="${r.getRequiredDepositAmount()}" type="number" groupingUsed="true"/>đ)
        </button>
    </form>
</c:if>
<c:if test="${r.status eq 'QUOTED'}"><form method="post" action="${pageContext.request.contextPath}/my-designs"><input type="hidden" name="csrfToken" value="${csrfTokenValue}"><input type="hidden" name="action" value="declineQuote"><input type="hidden" name="requestId" value="${r.id}"><button class="uv2-button is-light" type="submit">Từ chối báo giá</button></form></c:if>
<c:if test="${r.status eq 'QUOTED' or r.status eq 'CUSTOMER_DECLINED' or r.status eq 'EXPIRED' or r.status eq 'CLOSED'}"><form method="post" action="${pageContext.request.contextPath}/my-designs"><input type="hidden" name="csrfToken" value="${csrfTokenValue}"><input type="hidden" name="action" value="newQuoteDraft"><input type="hidden" name="requestId" value="${r.id}"><button class="uv2-button is-light" type="submit">Yêu cầu báo giá từ xưởng khác</button></form></c:if>
</div></article></c:forEach></div></c:otherwise></c:choose></section></div></div></main><jsp:include page="/customer/footer.jsp"/></body></html>
