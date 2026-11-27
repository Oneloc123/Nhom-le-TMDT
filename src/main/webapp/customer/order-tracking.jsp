<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Theo dõi đơn hàng | PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261127-order-tracking-r2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customer-modern.css?v=20261125-1">
</head>
<body>
<jsp:include page="/customer/header.jsp"/>
<main class="uv2-page account-tracking-page" data-tracking-code="<c:out value='${order.orderCode}'/>" data-context-path="${pageContext.request.contextPath}">
    <div class="uv2-breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span>
        <a href="${pageContext.request.contextPath}/purchase-history">Đơn hàng của tôi</a><span>/</span>
        <span>Theo dõi đơn</span>
    </div>
    <div class="uv2-page-heading">
        <div>
            <p class="uv2-eyebrow">THEO DÕI ĐƠN HÀNG</p>
            <h1><c:out value="${order.orderCode}"/></h1>
            <p>Cập nhật gần nhất từ đơn hàng, xưởng in và vận chuyển.</p>
        </div>
        <a class="uv2-button is-primary" href="${pageContext.request.contextPath}/order-detail?code=${order.orderCode}">Chi tiết đơn ↗</a>
    </div>
    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>
        <div class="uv2-content uv2-tracking-content">
            <section class="account-tracking-summary" aria-live="polite">
                <div><span>Trạng thái đơn</span><strong id="trackingStatus"><c:out value="${order.status}"/></strong></div>
                <div><span>Thanh toán</span><strong id="trackingPayment"><c:out value="${order.paymentStatus}"/></strong></div>
                <div><span>Vận chuyển</span><strong id="trackingShipping"><c:out value="${order.shippingStatus}"/></strong></div>
                <div>
                    <span>Ngày dự kiến hoàn thành</span>
                    <strong id="trackingEstimatedDelivery">
                        <c:choose>
                            <c:when test="${not empty estimatedDeliveryDisplay}"><c:out value="${estimatedDeliveryDisplay}"/></c:when>
                            <c:otherwise>Chưa xác định</c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </section>

            <c:if test="${not empty deliveryShipments}">
                <section class="uv2-panel delivery-receipt-panel">
                    <div class="uv2-panel-head">
                        <div>
                            <p class="uv2-eyebrow">XÁC NHẬN GIAO HÀNG</p>
                            <h2>Kiểm tra các kiện hàng</h2>
                        </div>
                    </div>
                    <c:if test="${param.receiptConfirmed == '1'}">
                        <div class="uv2-alert is-success" role="status">Đã ghi nhận xác nhận nhận hàng của bạn.</div>
                    </c:if>
                    <c:if test="${param.disputeCreated == '1'}">
                        <div class="uv2-alert is-success" role="status">Khiếu nại giao hàng đã được tiếp nhận. Đơn hàng sẽ tạm dừng hoàn tất trong thời gian xử lý.</div>
                    </c:if>
                    <c:if test="${not empty param.deliveryError}">
                        <div class="uv2-alert is-error" role="alert"><c:out value="${param.deliveryError}"/></div>
                    </c:if>
                    <div class="delivery-receipt-list">
                        <c:forEach var="deliveryShipment" items="${deliveryShipments}">
                            <article class="delivery-receipt-card">
                                <div class="delivery-receipt-card-head">
                                    <div>
                                        <strong>Kiện hàng #<c:out value="${deliveryShipment.id}"/></strong>
                                        <p><c:out value="${deliveryShipment.shopName}"/></p>
                                    </div>
                                    <span class="uv2-status"><c:out value="${deliveryShipment.receiptStatus}"/></span>
                                </div>
                                <p class="tracking-share-desc">
                                    <c:out value="${deliveryShipment.carrier}"/> · Mã vận đơn:
                                    <c:out value="${deliveryShipment.trackingNumber}"/> · Trạng thái:
                                    <c:out value="${deliveryShipment.shipmentStatus}"/>
                                </p>
                                <c:choose>
                                    <c:when test="${deliveryShipment.receiptStatus eq 'WAITING'}">
                                        <form method="post" action="${pageContext.request.contextPath}/order-tracking" class="delivery-confirm-form">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="confirmReceipt">
                                            <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                                            <input type="hidden" name="shipmentId" value="<c:out value='${deliveryShipment.id}'/>">
                                            <button class="uv2-button is-primary" type="submit">Tôi đã nhận kiện hàng này</button>
                                        </form>
                                        <details class="delivery-issue-details">
                                            <summary>Tôi chưa nhận được hàng hoặc hàng có vấn đề</summary>
                                            <form method="post" action="${pageContext.request.contextPath}/order-tracking" class="delivery-issue-form">
                                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                <input type="hidden" name="action" value="reportDeliveryIssue">
                                                <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                                                <input type="hidden" name="shipmentId" value="<c:out value='${deliveryShipment.id}'/>">
                                                <label>Loại vấn đề
                                                    <select name="issueType" required>
                                                        <option value="NOT_RECEIVED">Tôi chưa nhận được hàng</option>
                                                        <option value="DAMAGED">Hàng bị hư hỏng</option>
                                                        <option value="MISSING">Hàng bị thiếu</option>
                                                        <option value="WRONG_ITEM">Giao sai sản phẩm</option>
                                                        <option value="OTHER">Vấn đề khác</option>
                                                    </select>
                                                </label>
                                                <label>Mô tả chi tiết
                                                    <textarea name="description" rows="3" minlength="10" maxlength="2000" required
                                                        placeholder="Mô tả tình trạng bạn gặp phải (ít nhất 10 ký tự)"></textarea>
                                                </label>
                                                <button class="uv2-button is-light" type="submit">Gửi khiếu nại</button>
                                            </form>
                                        </details>
                                    </c:when>
                                    <c:when test="${deliveryShipment.receiptStatus eq 'CONFIRMED'}">
                                        <div class="uv2-alert is-success">Bạn đã xác nhận nhận kiện hàng này.</div>
                                    </c:when>
                                    <c:when test="${deliveryShipment.receiptStatus eq 'DISPUTED'}">
                                        <div class="uv2-alert is-error">Kiện hàng đang có khiếu nại. Bạn sẽ có thể xác nhận sau khi Admin cập nhật kết quả.</div>
                                    </c:when>
                                    <c:otherwise>
                                        <p class="account-muted-note">Kiện hàng chưa đến bước khách xác nhận.</p>
                                    </c:otherwise>
                                </c:choose>
                            </article>
                        </c:forEach>
                    </div>
                </section>
            </c:if>

            <section class="uv2-panel account-timeline">
                <div class="uv2-panel-head">
                    <div>
                        <p class="uv2-eyebrow">TIẾN ĐỘ XỬ LÝ</p>
                        <h2>Hành trình đơn hàng</h2>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${empty trackingEvents}">
                        <div class="uv2-empty">
                            <b>Chưa có cập nhật tiến độ</b>
                            <p>Các mốc xử lý từ xưởng in và đơn vị vận chuyển sẽ xuất hiện tại đây.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <ol id="trackingTimeline" class="tracking-timeline">
                            <c:forEach var="event" items="${trackingEvents}">
                                <li>
                                    <div>
                                        <strong><c:out value="${event.title}"/></strong>
                                        <c:if test="${not empty event.details}">
                                            <p><c:out value="${event.details}"/></p>
                                        </c:if>
                                        <small><c:out value="${event.source}"/> · <c:out value="${event.when}"/></small>
                                    </div>
                                </li>
                            </c:forEach>
                        </ol>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="uv2-panel tracking-share-panel">
                <div class="uv2-panel-head">
                    <div>
                        <p class="uv2-eyebrow">LIÊN KẾT CÔNG KHAI</p>
                        <h2>Liên kết chia sẻ trạng thái</h2>
                    </div>
                </div>
                <p class="tracking-share-desc">Người có liên kết chỉ xem mã đơn, trạng thái và mốc tiến độ cơ bản; không xem số điện thoại hoặc địa chỉ nhận hàng.</p>
                <c:if test="${param.shareUpdated == '1'}">
                    <div class="uv2-alert is-success" role="status">Đã cập nhật cài đặt chia sẻ trạng thái đơn hàng.</div>
                </c:if>
                <c:if test="${param.shareError == '1'}">
                    <div class="uv2-alert is-error" role="alert">Không cập nhật được cài đặt chia sẻ. Vui lòng thử lại.</div>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/order-tracking" class="tracking-share-form">
                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                    <input type="hidden" name="action" value="toggleShare">
                    <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                    <label>
                        <input type="checkbox" name="enabled" value="true" <c:if test="${trackingShareEnabled}">checked</c:if>>
                        <span>Cho phép truy cập liên kết chia sẻ</span>
                    </label>
                    <button class="uv2-button is-light" type="submit">Lưu cài đặt chia sẻ</button>
                </form>
                <c:if test="${trackingShareEnabled}">
                    <div class="tracking-share-url">
                        <input id="trackingShareUrl" type="text" readonly value="<c:out value='${trackingShareUrl}'/>" aria-label="Liên kết chia sẻ trạng thái đơn hàng">
                        <button class="uv2-button is-primary" type="button" data-copy-tracking>Copy liên kết</button>
                    </div>
                </c:if>
            </section>

            <p class="account-muted-note" id="trackingRefreshStatus" aria-live="polite">Trang tự kiểm tra cập nhật khoảng 30 giây một lần khi còn mở.</p>
        </div>
    </div>
</main>
<jsp:include page="/customer/footer.jsp"/>
<script src="${pageContext.request.contextPath}/js/order-tracking.js" defer></script>
</body>
</html>
