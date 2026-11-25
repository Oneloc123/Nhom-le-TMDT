<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thanh toán đặt cọc ZaloPay - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261104-1">
</head>
<body>
<%@ include file="header.jsp" %>
<main class="shop-page payment-result-page">
    <section class="payment-result-card payment-result-pending">
        <div class="payment-result-icon" aria-hidden="true">₫</div>
        <h1>Đặt cọc 30% qua ZaloPay Sandbox</h1>
        <p class="payment-result-message" id="zalopayWaitMessage">
            Mở cổng thanh toán ZaloPay ở tab mới để hoàn tất đặt cọc. Trang này sẽ tự động cập nhật và chuyển về PrintKraft ngay khi giao dịch được xác nhận.
        </p>

        <dl class="payment-result-details">
            <c:if test="${not empty quoteRequestId}">
                <div><dt>Mã yêu cầu báo giá</dt><dd>#<c:out value="${quoteRequestId}"/></dd></div>
            </c:if>
            <c:if test="${not empty requestCode}">
                <div><dt>Mã yêu cầu in 3D</dt><dd><c:out value="${requestCode}"/></dd></div>
            </c:if>
            <c:if test="${not empty productName}">
                <div><dt>Sản phẩm / Mô hình</dt><dd><c:out value="${productName}"/></dd></div>
            </c:if>
            <c:if test="${not empty quotedAmount}">
                <div><dt>Tổng giá xưởng báo</dt><dd><fmt:formatNumber value="${quotedAmount}" type="number" groupingUsed="true"/>đ</dd></div>
            </c:if>
            <c:if test="${not empty amount}">
                <div><dt>Số tiền đặt cọc (30%)</dt><dd><b><fmt:formatNumber value="${amount}" type="number" groupingUsed="true"/>đ</b></dd></div>
            </c:if>
            <div><dt>Mã giao dịch</dt><dd><c:out value="${appTransId}"/></dd></div>
        </dl>

        <div class="button-row payment-result-actions">
            <a class="button-primary" style="background:#0068ff;border-color:#0068ff;" href="<c:out value='${orderUrl}'/>" target="_blank" rel="noopener" id="openZaloPayBtn">
                Mở cổng thanh toán ZaloPay ↗
            </a>
            <button type="button" class="button-secondary" id="checkStatusNowBtn">Kiểm tra thanh toán ngay</button>
            <a class="button-secondary" href="${pageContext.request.contextPath}${backUrl}"><c:out value="${backLabel}"/></a>
        </div>
        <p class="muted payment-result-note">
            Hoặc <a href="<c:out value='${orderUrl}'/>" style="text-decoration:underline;">bấm vào đây để mở ZaloPay trên tab hiện tại</a>.
        </p>
    </section>
</main>
<%@ include file="footer.jsp" %>
<script>
(() => {
    const statusUrl = "${pageContext.request.contextPath}/zalopay/status?appTransId=" + encodeURIComponent("${appTransId}");
    const messageEl = document.getElementById("zalopayWaitMessage");
    const checkBtn = document.getElementById("checkStatusNowBtn");
    let checking = false;
    let stopped = false;

    async function checkStatus(manual) {
        if (stopped || checking) return;
        checking = true;
        if (manual && checkBtn) checkBtn.disabled = true;
        try {
            const resp = await fetch(statusUrl, { cache: "no-store", credentials: "same-origin" });
            if (resp.ok) {
                const data = await resp.json();
                if (data.status === "PAID" || data.status === "FAILED") {
                    stopped = true;
                    window.location.href = data.returnUrl;
                    return;
                }
                if (manual && messageEl) {
                    messageEl.textContent = "Giao dịch chưa được xác nhận hoàn tất trên ZaloPay. Nếu bạn vừa thanh toán xong, hãy chờ 1–2 giây rồi bấm lại.";
                }
            }
        } catch (ignored) {
        } finally {
            checking = false;
            if (checkBtn) checkBtn.disabled = false;
        }
    }

    if (checkBtn) {
        checkBtn.addEventListener("click", () => checkStatus(true));
    }
    window.setInterval(() => checkStatus(false), 2500);
})();
</script>
</body>
</html>
