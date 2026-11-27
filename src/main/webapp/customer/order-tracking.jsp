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

            <c:if test="${param.receiptConfirmed == '1'}">
                <p class="uv2-alert is-success" role="status">Đã ghi nhận hoàn thành đơn hàng.</p>
            </c:if>
            <c:if test="${param.receiptError == '1'}">
                <p class="uv2-alert is-error" role="alert">Chưa thể xác nhận. Kiểm tra trạng thái giao hàng và yêu cầu đang mở.</p>
            </c:if>
            <section id="ghnReceipt" class="uv2-panel" ${empty ghnReceipt ? 'hidden' : ''}>
                <h2>Vận đơn GHN</h2>
                <p>Mã vận đơn <strong id="ghnTrackingNumber"><c:out value="${ghnReceipt.trackingNumber}"/></strong></p>
                <div id="ghnReceiptActions" ${ghnReceipt.awaitingConfirmation ? '' : 'hidden'}>
                    <h3>Đã giao – chờ xác nhận</h3>
                    <p id="ghnDeadlineText" ${ghnReceipt.openCase ? 'hidden' : ''}>Đơn sẽ tự hoàn thành sau <strong id="ghnDeadline"><c:out value="${ghnReceipt.deadline}"/></strong> nếu không có khiếu nại đang mở.</p>
                    <p id="ghnOpenCase" ${ghnReceipt.openCase ? '' : 'hidden'}>Đơn đang có yêu cầu hỗ trợ hoặc đổi trả chưa xử lý xong.</p>
                    <form id="ghnConfirmForm" method="post" action="${pageContext.request.contextPath}/order-tracking" ${ghnReceipt.openCase ? 'hidden' : ''}>
                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                        <input type="hidden" name="action" value="confirmReceipt">
                        <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                        <button class="uv2-button is-primary" type="submit">Đã nhận đủ và đúng hàng</button>
                    </form>
                    <a class="uv2-button is-light" href="${pageContext.request.contextPath}/support?category=COMPLAINT&amp;orderId=${order.orderId}">Báo có vấn đề</a>
                </div>
            </section>
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
<script src="${pageContext.request.contextPath}/js/order-tracking.js?v=ghn-delivery-v1" defer></script>
</body>
</html>
