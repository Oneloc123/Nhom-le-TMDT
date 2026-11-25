<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết đơn hàng | PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customer-modern.css?v=20261125-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/order-cancellation.css?v=20261125-1">
</head>
<body>
<jsp:include page="/customer/header.jsp"/>
<main class="uv2-page">
    <div class="uv2-breadcrumb">
        <a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span>
        <a href="${pageContext.request.contextPath}/purchase-history">Đơn hàng</a><span>/</span><span>Chi tiết</span>
    </div>
    <div class="uv2-page-heading">
        <div>
            <p class="uv2-eyebrow">CHI TIẾT ĐƠN HÀNG</p>
            <h1><c:out value="${order.orderCode}"/></h1>
            <p>Đặt lúc <c:out value="${order.placedAt}"/> · Giao hàng: <b><c:out value="${order.shippingStatus}"/></b></p>
        </div>
        <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;">
            <a class="uv2-button is-primary" href="${pageContext.request.contextPath}/order-tracking?code=${order.orderCode}">Theo dõi giao hàng ↗</a>
            <span class="uv2-status"><c:out value="${order.status}"/></span>
        </div>
    </div>

    <c:if test="${param.cancelled eq '1'}">
        <div class="uv2-alert is-success oc-page-alert" role="status" aria-live="polite">
            <strong>Đã hủy đơn hàng thành công.</strong>
            Yêu cầu đã được ghi nhận và trạng thái đơn đã được cập nhật.
            <c:if test="${order.paymentStatus eq 'Cần hoàn tiền' or order.paymentStatus eq 'Chờ hoàn tiền'}">
                <span>Đơn hàng đã thanh toán đang ở trạng thái cần hoàn tiền. Thao tác hủy không tự động chuyển tiền về tài khoản của bạn.</span>
            </c:if>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="uv2-alert is-error oc-page-alert" role="alert">
            <strong>Chưa thể hủy đơn hàng.</strong> <c:out value="${error}"/>
        </div>
    </c:if>

    <div class="uv2-detail-grid">
        <div class="uv2-content">
            <section class="uv2-panel">
                <div class="uv2-panel-head">
                    <div><p class="uv2-eyebrow">SẢN PHẨM</p><h2>Các mặt hàng trong đơn</h2></div>
                </div>
                <div class="uv2-table-wrap">
                    <table class="uv2-table">
                        <thead><tr><th>Sản phẩm</th><th>Xưởng</th><th>SL</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead>
                        <tbody>
                        <c:forEach var="item" items="${order.items}">
                            <tr>
                                <td><b><c:out value="${item.productName}"/></b><small class="uv2-cell-sub"><c:out value="${item.status}"/></small></td>
                                <td>
                                    <c:out value="${item.shopName}"/>
                                    <c:if test="${not empty item.shopId}">
                                        <a class="uv2-cell-sub oc-shop-message" href="${pageContext.request.contextPath}/messages?orderId=${order.orderId}&amp;shopId=${item.shopId}">💬 Nhắn tin xưởng</a>
                                    </c:if>
                                </td>
                                <td><c:out value="${item.quantity}"/></td>
                                <td><c:out value="${item.unitPrice}"/> ₫</td>
                                <td><b><c:out value="${item.lineTotal}"/> ₫</b></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="uv2-panel">
                <div class="uv2-panel-head">
                    <div><p class="uv2-eyebrow">TIẾN ĐỘ</p><h2>Lịch sử trạng thái</h2></div>
                </div>
                <div class="uv2-timeline">
                    <c:forEach var="event" items="${order.timeline}">
                        <article>
                            <span class="uv2-timeline-dot"></span>
                            <div>
                                <b><c:out value="${event.newStatus}"/></b>
                                <small><c:out value="${event.changedAt}"/></small>
                                <p><c:out value="${event.note}"/></p>
                            </div>
                        </article>
                    </c:forEach>
                    <c:if test="${empty order.timeline}"><p class="uv2-muted">Chưa có cập nhật trạng thái.</p></c:if>
                </div>
            </section>
        </div>

        <aside class="uv2-content">
            <section class="uv2-panel">
                <div class="uv2-panel-head">
                    <div><p class="uv2-eyebrow">TÓM TẮT</p><h2>Thanh toán</h2></div>
                </div>
                <div class="uv2-total-row"><span>Tạm tính</span><b><c:out value="${order.subtotal}"/> ₫</b></div>
                <div class="uv2-total-row"><span>Phí vận chuyển</span><b><c:out value="${order.shippingFee}"/> ₫</b></div>
                <div class="uv2-total-row is-grand"><span>Tổng cộng</span><b><c:out value="${order.totalAmount}"/> ₫</b></div>
                <p class="uv2-muted">Trạng thái thanh toán: <b><c:out value="${order.paymentStatus}"/></b></p>
            </section>

            <section class="uv2-panel">
                <div class="uv2-panel-head">
                    <div><p class="uv2-eyebrow">GIAO HÀNG</p><h2>Người nhận</h2></div>
                </div>
                <b><c:out value="${order.recipientName}"/></b>
                <p><c:out value="${order.recipientPhone}"/></p>
                <p><c:out value="${order.shippingAddress}"/></p>
                <c:if test="${not empty order.customerNote}"><small>Ghi chú: <c:out value="${order.customerNote}"/></small></c:if>
            </section>

            <c:choose>
                <c:when test="${order.canCancel}">
                    <section class="uv2-panel oc-cancel-panel">
                        <div class="oc-cancel-heading">
                            <span class="oc-cancel-icon" aria-hidden="true">!</span>
                            <div><p class="uv2-eyebrow">CẦN THAY ĐỔI?</p><h2>Hủy đơn hàng</h2></div>
                        </div>
                        <p class="oc-cancel-intro">Bạn có thể hủy đơn khi xưởng chưa bắt đầu xử lý. Hãy kiểm tra thông tin trước khi xác nhận.</p>
                        <div class="oc-cancel-facts">
                            <div><span>Mã đơn</span><strong><c:out value="${order.orderCode}"/></strong></div>
                            <div><span>Trạng thái</span><strong><c:out value="${order.status}"/></strong></div>
                        </div>
                        <form id="cancelOrderForm" class="oc-cancel-form" method="post"
                              action="${pageContext.request.contextPath}/order-detail"
                              data-reopen="${not empty error}">
                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                            <input type="hidden" name="action" value="cancel">
                            <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                            <button class="uv2-button is-danger oc-open-dialog" type="button" data-open-cancel-dialog>
                                <span aria-hidden="true">×</span> Tiếp tục hủy đơn
                            </button>
                            <dialog class="oc-dialog" id="cancelOrderDialog" aria-labelledby="ocDialogTitle">
                                <div class="oc-dialog-content">
                                    <div class="oc-dialog-heading">
                                        <span class="oc-dialog-symbol" aria-hidden="true">!</span>
                                        <div>
                                            <p class="uv2-eyebrow">XÁC NHẬN THAO TÁC</p>
                                            <h2 id="ocDialogTitle">Bạn chắc chắn muốn hủy đơn?</h2>
                                        </div>
                                        <button type="button" class="oc-dialog-close" data-close-cancel-dialog aria-label="Đóng hộp xác nhận">×</button>
                                    </div>
                                    <p class="oc-dialog-lead">Hãy kiểm tra lại đơn hàng và cung cấp lý do để xưởng có thể cập nhật quy trình.</p>
                                    <div class="oc-dialog-order">
                                        <div><span>Mã đơn hàng</span><strong><c:out value="${order.orderCode}"/></strong></div>
                                        <div><span>Tổng giá trị</span><strong><c:out value="${order.totalAmount}"/> ₫</strong></div>
                                        <div><span>Trạng thái đơn</span><strong><c:out value="${order.status}"/></strong></div>
                                        <div><span>Thanh toán</span><strong><c:out value="${order.paymentStatus}"/></strong></div>
                                    </div>
                                    <label class="oc-reason-label" for="cancelReason">Lý do hủy <span>Bắt buộc</span></label>
                                    <div class="oc-reason-suggestions" aria-label="Lý do gợi ý">
                                        <button type="button" data-reason-suggestion="Đặt nhầm sản phẩm">Đặt nhầm sản phẩm</button>
                                        <button type="button" data-reason-suggestion="Muốn thay đổi thông tin đơn hàng">Thay đổi thông tin</button>
                                        <button type="button" data-reason-suggestion="Thời gian giao hàng chưa phù hợp">Thời gian giao hàng</button>
                                        <button type="button" data-reason-suggestion="Tôi đã thay đổi nhu cầu mua hàng">Đổi nhu cầu</button>
                                    </div>
                                    <textarea id="cancelReason" name="reason" rows="3" maxlength="500" required
                                              placeholder="Chọn lý do gợi ý hoặc nhập lý do của bạn (tối đa 500 ký tự)."><c:out value="${cancelReasonInput}"/></textarea>
                                    <div class="oc-reason-meta">
                                        <small>Thông tin này được lưu trong lịch sử đơn hàng.</small>
                                        <span><b data-reason-count>0</b>/500</span>
                                    </div>
                                    <div class="oc-payment-warning" role="note">
                                        <span aria-hidden="true">ⓘ</span>
                                        <p>Nếu đơn đã thanh toán, hệ thống sẽ ghi nhận trạng thái cần hoàn tiền. Thao tác hủy không tự động chuyển tiền về tài khoản của bạn.</p>
                                    </div>
                                    <div class="oc-dialog-actions">
                                        <button type="button" class="uv2-button is-light" data-close-cancel-dialog>Giữ lại đơn</button>
                                        <button type="submit" class="uv2-button is-danger" data-confirm-cancel>Xác nhận hủy đơn</button>
                                    </div>
                                </div>
                            </dialog>
                        </form>
                    </section>
                </c:when>
                <c:when test="${order.status eq 'Đã hủy'}">
                    <section class="uv2-panel oc-cancelled-panel">
                        <div class="oc-cancel-heading">
                            <span class="oc-cancel-icon is-complete" aria-hidden="true">✓</span>
                            <div><p class="uv2-eyebrow">TRẠNG THÁI HỦY</p><h2>Đơn hàng đã hủy</h2></div>
                        </div>
                        <p class="oc-cancel-intro">Đơn hàng này đã được hủy trước đó.</p>
                        <c:if test="${not empty order.cancelReason}">
                            <div class="oc-saved-reason"><span>Lý do đã ghi nhận</span><p><c:out value="${order.cancelReason}"/></p></div>
                        </c:if>
                        <c:if test="${order.paymentStatus eq 'Cần hoàn tiền' or order.paymentStatus eq 'Chờ hoàn tiền'}">
                            <div class="oc-payment-warning" role="note"><span aria-hidden="true">ⓘ</span><p>Đơn đã thanh toán và đang chờ xử lý hoàn tiền. Khoản tiền không được tự động chuyển trong thao tác hủy.</p></div>
                        </c:if>
                    </section>
                </c:when>
                <c:when test="${order.status ne 'Đã hoàn thành'}">
                    <section class="uv2-panel oc-cancel-unavailable">
                        <div class="oc-cancel-heading">
                            <span class="oc-cancel-icon is-muted" aria-hidden="true">i</span>
                            <div><p class="uv2-eyebrow">HỖ TRỢ ĐƠN HÀNG</p><h2>Không thể hủy trực tiếp</h2></div>
                        </div>
                        <p class="oc-cancel-intro"><c:out value="${order.cancelUnavailableReason}"/></p>
                        <a class="uv2-button is-light" href="${pageContext.request.contextPath}/support">Liên hệ hỗ trợ</a>
                    </section>
                </c:when>
            </c:choose>
        </aside>
    </div>
    <a class="uv2-back-link" href="${pageContext.request.contextPath}/purchase-history">← Quay lại danh sách đơn hàng</a>
</main>
<jsp:include page="/customer/footer.jsp"/>
<script src="${pageContext.request.contextPath}/js/order-cancellation.js?v=20261125-1" defer></script>
</body>
</html>
