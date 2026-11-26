<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Đổi trả & hoàn tiền | PrintKraft</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261126-customer-sync-r1">
<style>
.rr-grid{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(300px,.8fr);gap:18px;align-items:start}
.rr-list{display:grid;gap:12px}.rr-card{border:1px solid #e4eaf3;border-radius:14px;padding:16px;background:#fff}
.rr-card-head{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.rr-title{font-weight:800;color:#1d2942}
.rr-muted{color:#6e7b92;font-size:13px}.rr-form{display:grid;gap:12px}.rr-form label{display:grid;gap:6px;font-size:13px;font-weight:700}
.rr-form input,.rr-form select,.rr-form textarea{width:100%;min-width:0;border:1px solid #dbe3f0;border-radius:9px;padding:10px 11px;color:#1d2942;background:#fff;font:inherit}
.rr-form textarea{min-height:94px;resize:vertical}.rr-actions{display:flex;gap:9px;flex-wrap:wrap;margin-top:10px}
.rr-btn{border:0;border-radius:9px;padding:10px 13px;background:#315be8;color:#fff;font-weight:750;cursor:pointer;text-decoration:none;display:inline-block}
.rr-btn.secondary{background:#eef2ff;color:#304fc4}.rr-btn.danger{background:#fff0f0;color:#b32737}
.rr-status{display:inline-block;border-radius:999px;background:#eef2ff;color:#304fc4;padding:5px 9px;font-size:11px;font-weight:800}
.rr-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.rr-field{padding:10px;border-radius:10px;background:#f7f9fd;overflow-wrap:anywhere}
.rr-field small{display:block;color:#6e7b92;margin-bottom:4px}.rr-field strong{font-size:13px}.rr-timeline{display:grid;gap:0}.rr-event{padding:12px 0;border-bottom:1px solid #e4eaf3}.rr-event:last-child{border-bottom:0}
.rr-evidence{display:flex;gap:10px;flex-wrap:wrap}.rr-evidence a{width:108px;color:#315be8;font-size:12px;overflow-wrap:anywhere}.rr-evidence img{width:104px;height:84px;object-fit:cover;border-radius:8px;border:1px solid #dbe3f0}
.rr-alert{border-radius:10px;padding:12px 14px;margin-bottom:14px;background:#fff5e6;color:#8a4c00}.rr-success{background:#eaf8ef;color:#17673c}
.rr-policy{color:#6e7b92;font-size:12px;line-height:1.6}.rr-empty{padding:18px;border:1px dashed #ccd6e8;border-radius:12px;color:#6e7b92}
@media(max-width:960px){.rr-grid{grid-template-columns:minmax(0,1fr)}}@media(max-width:560px){.rr-fields{grid-template-columns:minmax(0,1fr)}}
</style>
</head>
<body>
<jsp:include page="/customer/header.jsp"/>
<main class="uv2-page">
  <div class="uv2-breadcrumb"><a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span><span>Đổi trả & hoàn tiền</span></div>
  <div class="uv2-page-heading"><div><p class="uv2-eyebrow">SAU MUA HÀNG</p><h1>Đổi trả & hoàn tiền</h1><p>Gửi yêu cầu gắn với đúng sản phẩm trong đơn hàng, tải ảnh bằng chứng và theo dõi từng bước xử lý.</p></div></div>
  <div class="uv2-account-grid">
    <jsp:include page="account-sidebar.jsp"/>
    <div class="uv2-content">
      <c:if test="${not empty error}"><div class="rr-alert" role="alert"><c:out value="${error}"/></div></c:if>
      <c:if test="${param.created eq '1' or param.evidenceAdded eq '1' or param.withdrawn eq '1' or param.shipped eq '1'}"><div class="rr-alert rr-success" role="status">Thao tác đã được lưu.</div></c:if>
      <div class="rr-grid">
        <section class="uv2-panel">
          <div class="uv2-panel-head"><div><p class="uv2-eyebrow">YÊU CẦU CỦA BẠN</p><h2>Lịch sử đổi trả / hoàn tiền</h2></div></div>
          <div class="rr-list">
            <c:choose><c:when test="${empty returnRefundRequests}"><div class="rr-empty">Bạn chưa có yêu cầu đổi trả hoặc hoàn tiền nào.</div></c:when>
            <c:otherwise><c:forEach var="r" items="${returnRefundRequests}">
              <article class="rr-card">
                <div class="rr-card-head"><div><div class="rr-title"><c:out value="${r.requestCode}"/></div><div class="rr-muted"><c:out value="${r.orderCode}"/> · <c:out value="${r.productNameSnapshot}"/></div></div><span class="rr-status"><c:out value="${r.statusLabel}"/></span></div>
                <p class="rr-muted">Số lượng: <b><c:out value="${r.requestedQuantity}"/></b> · Yêu cầu: <fmt:formatNumber value="${r.requestedAmount}" groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/> ₫</p>
                <div class="rr-actions"><a class="rr-btn secondary" href="${pageContext.request.contextPath}/return-refunds?id=${r.requestId}">Xem tiến trình</a></div>
              </article>
            </c:forEach></c:otherwise></c:choose>
          </div>
        </section>
        <section class="uv2-panel">
          <div class="uv2-panel-head"><div><p class="uv2-eyebrow">ĐIỀU KIỆN TIẾP NHẬN</p><h2>Tạo yêu cầu</h2></div></div>
          <p class="rr-policy">Yêu cầu trong vòng 7 ngày từ khi giao thành công. Hệ thống kiểm tra đơn và số lượng còn đủ điều kiện; sản phẩm tùy chỉnh chỉ tiếp nhận lý do lỗi, hư hỏng, giao sai hoặc thiếu hàng. Ảnh JPEG/PNG tối đa 5 MB/ảnh.</p>
          <c:choose><c:when test="${empty eligibleItems}"><div class="rr-empty">Hiện không có sản phẩm nào đủ điều kiện đổi trả theo thời hạn và số lượng còn lại.</div></c:when>
          <c:otherwise><form class="rr-form" method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/return-refunds">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="create">
            <label>Sản phẩm trong đơn hàng<select name="orderItemId" required><c:forEach var="item" items="${eligibleItems}"><option value="${item.orderItemId}"><c:out value="${item.orderCode}"/> · <c:out value="${item.productName}"/> (còn <c:out value="${item.availableQuantity}"/> sản phẩm, đến <c:out value="${item.deadline}"/>)</option></c:forEach></select></label>
            <label>Loại yêu cầu<select name="requestType" required><option value="RETURN_AND_REFUND">Trả hàng và hoàn tiền</option><option value="REFUND_ONLY">Chỉ yêu cầu hoàn tiền (không gửi trả)</option></select></label>
            <label>Lý do<select name="reasonCode" required><option value="DEFECTIVE">Sản phẩm bị lỗi</option><option value="DAMAGED">Hư hỏng khi nhận</option><option value="WRONG_ITEM">Giao sai sản phẩm</option><option value="MISSING_ITEM">Thiếu sản phẩm</option><option value="CHANGED_MIND">Thay đổi quyết định</option><option value="OTHER">Lý do khác</option></select></label>
            <label>Số lượng cần yêu cầu<input type="number" name="quantity" min="1" max="100000" value="1" required></label>
            <label>Mô tả chi tiết<textarea name="customerNote" minlength="10" maxlength="2000" required placeholder="Mô tả tình trạng, thời điểm phát hiện và cách bạn muốn được hỗ trợ."></textarea></label>
            <label>Ảnh bằng chứng (JPEG/PNG, tối đa 4 ảnh)<input type="file" name="evidence" accept="image/jpeg,image/png,.jpg,.jpeg,.png" multiple></label>
            <button class="rr-btn" type="submit">Gửi yêu cầu</button>
          </form></c:otherwise></c:choose>
        </section>
      </div>

      <c:if test="${not empty selectedRequest}">
        <section class="uv2-panel" style="margin-top:18px">
          <div class="uv2-panel-head"><div><p class="uv2-eyebrow">CHI TIẾT YÊU CẦU</p><h2><c:out value="${selectedRequest.requestCode}"/></h2></div><span class="rr-status"><c:out value="${selectedRequest.statusLabel}"/></span></div>
          <div class="rr-fields">
            <div class="rr-field"><small>Đơn hàng</small><strong><c:out value="${selectedRequest.orderCode}"/></strong></div>
            <div class="rr-field"><small>Sản phẩm</small><strong><c:out value="${selectedRequest.productNameSnapshot}"/></strong></div>
            <div class="rr-field"><small>Loại yêu cầu</small><strong><c:out value="${selectedRequest.requestType}"/></strong></div>
            <div class="rr-field"><small>Lý do</small><strong><c:out value="${selectedRequest.reasonCode}"/></strong></div>
            <div class="rr-field"><small>Số lượng / số tiền yêu cầu</small><strong><c:out value="${selectedRequest.requestedQuantity}"/> · <fmt:formatNumber value="${selectedRequest.requestedAmount}" groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/> ₫</strong></div>
            <div class="rr-field"><small>Số tiền được duyệt</small><strong><c:choose><c:when test="${not empty selectedRequest.approvedAmount}"><fmt:formatNumber value="${selectedRequest.approvedAmount}" groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/> ₫</c:when><c:otherwise>Chưa có quyết định</c:otherwise></c:choose></strong></div>
            <div class="rr-field"><small>Gửi lúc</small><strong><c:out value="${selectedRequest.submittedAt}"/></strong></div>
            <div class="rr-field"><small>Phiên bản chính sách</small><strong><c:out value="${selectedRequest.policyVersion}"/></strong></div>
            <c:if test="${not empty selectedRequest.returnCarrier}"><div class="rr-field"><small>Gửi hàng trả</small><strong><c:out value="${selectedRequest.returnCarrier}"/> · <c:out value="${selectedRequest.returnTrackingNumber}"/> · <c:out value="${selectedRequest.returnShippedAt}"/></strong></div></c:if>
            <c:if test="${not empty selectedRequest.resolutionMethod}"><div class="rr-field"><small>Phương thức hoàn tiền</small><strong><c:out value="${selectedRequest.resolutionMethod}"/></strong></div></c:if>
            <c:if test="${not empty selectedRequest.refundReference}"><div class="rr-field"><small>Mã tham chiếu hoàn tiền</small><strong><c:out value="${selectedRequest.refundReference}"/></strong></div></c:if>
          </div>
          <h3 style="margin-top:18px">Nội dung gửi lên</h3><p><c:out value="${selectedRequest.customerNote}"/></p>
          <c:if test="${not empty selectedRequest.supportTicketId}">
            <p class="rr-muted">Bạn có thể trao đổi thêm qua kênh hỗ trợ: <a href="${pageContext.request.contextPath}/support?id=${selectedRequest.supportTicketId}">Mở ticket liên quan</a></p>
          </c:if>
          <c:if test="${not empty selectedRequest.decisionNote}"><h3>Ghi chú xử lý</h3><p><c:out value="${selectedRequest.decisionNote}"/></p></c:if>
          <h3 style="margin-top:18px">Bằng chứng</h3>
          <div class="rr-evidence"><c:forEach var="e" items="${requestEvidence}"><a href="${pageContext.request.contextPath}/return-refunds/evidence?evidenceId=${e.evidenceId}" target="_blank" rel="noopener"><img src="${pageContext.request.contextPath}/return-refunds/evidence?evidenceId=${e.evidenceId}" alt="Ảnh bằng chứng"><span><c:out value="${e.originalFilename}"/></span></a></c:forEach></div>
          <c:if test="${selectedRequest.status eq 'SUBMITTED' or selectedRequest.status eq 'UNDER_REVIEW'}">
            <form class="rr-form" method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/return-refunds" style="margin-top:16px">
              <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="addEvidence"><input type="hidden" name="requestId" value="<c:out value='${selectedRequest.requestId}'/>">
              <label>Bổ sung ảnh bằng chứng (tổng tối đa 8 ảnh)<input type="file" name="evidence" accept="image/jpeg,image/png,.jpg,.jpeg,.png" multiple required></label><button class="rr-btn secondary" type="submit">Bổ sung bằng chứng</button>
            </form>
          </c:if>
          <c:if test="${selectedRequest.status eq 'AWAITING_RETURN_SHIPMENT'}">
            <h3 style="margin-top:18px">Thông tin gửi hàng hoàn trả</h3>
            <form class="rr-form" method="post" action="${pageContext.request.contextPath}/return-refunds">
              <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="markShipped"><input type="hidden" name="requestId" value="<c:out value='${selectedRequest.requestId}'/>">
              <label>Đơn vị vận chuyển / hình thức gửi<input name="returnCarrier" maxlength="100" required placeholder="Ví dụ: Gửi tại điểm nhận hàng"></label>
              <label>Mã vận đơn / mã biên nhận<input name="returnTrackingNumber" maxlength="120" required></label>
              <button class="rr-btn" type="submit">Xác nhận đã gửi hàng trả</button>
            </form>
          </c:if>
          <c:if test="${selectedRequest.status eq 'SUBMITTED' or selectedRequest.status eq 'UNDER_REVIEW'}">
            <form class="rr-actions" method="post" action="${pageContext.request.contextPath}/return-refunds" onsubmit="return confirm('Bạn muốn rút yêu cầu này?')">
              <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="withdraw"><input type="hidden" name="requestId" value="<c:out value='${selectedRequest.requestId}'/>"><button class="rr-btn danger" type="submit">Rút yêu cầu</button>
            </form>
          </c:if>
          <h3 style="margin-top:20px">Dòng thời gian xử lý</h3>
          <div class="rr-timeline"><c:forEach var="ev" items="${requestEvents}"><div class="rr-event"><strong><c:out value="${ev.toStatusLabel}"/></strong><div class="rr-muted"><c:out value="${ev.createdAt}"/> · <c:out value="${ev.actorName}"/></div><p><c:out value="${ev.note}"/></p></div></c:forEach></div>
        </section>
      </c:if>
    </div>
  </div>
</main>
<jsp:include page="/customer/footer.jsp"/>
</body></html>
