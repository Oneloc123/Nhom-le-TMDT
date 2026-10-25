<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sổ địa chỉ nhận hàng — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <script src="${pageContext.request.contextPath}/js/account.js" defer></script>
</head>
<body class="customer-account-body">
<%@ include file="header.jsp" %>
<main class="account-page account-address-page">
    <div class="account-breadcrumb"><a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span><a href="${pageContext.request.contextPath}/profile">Tài khoản</a><span>/</span><span>Sổ địa chỉ</span></div>

    <section class="address-heading">
        <div>
            <span class="account-overline">THÔNG TIN GIAO NHẬN</span>
            <h1>Sổ địa chỉ nhận hàng</h1>
            <p>Lưu các địa chỉ thường dùng để đặt hàng nhanh hơn. Địa chỉ mặc định sẽ được ưu tiên khi đặt hàng.</p>
        </div>
        <a class="account-outline-button" href="${pageContext.request.contextPath}/profile">← Quay lại hồ sơ</a>
    </section>

    <c:if test="${param.saved == '1'}"><div class="alert alert-ok account-alert" role="status">Đã lưu địa chỉ thành công.</div></c:if>
    <c:if test="${param.deleted == '1'}"><div class="alert alert-ok account-alert" role="status">Đã xóa địa chỉ.</div></c:if>
    <c:if test="${not empty param.error}"><div class="alert alert-err account-alert" role="alert"><c:out value="${param.error}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-err account-alert" role="alert"><c:out value="${error}"/></div></c:if>

    <div class="address-summary-row">
        <div><span class="address-summary-icon" aria-hidden="true">⌖</span><div><b><c:out value="${fn:length(addresses)}"/> địa chỉ đã lưu</b><p>Thông tin chỉ hiển thị trong tài khoản của bạn.</p></div></div>
        <label class="address-search"><span aria-hidden="true">⌕</span><input type="search" id="addressSearch" placeholder="Tìm theo tên, số điện thoại, địa chỉ..." aria-label="Tìm địa chỉ đã lưu"></label>
    </div>

    <div class="address-layout">
        <section class="address-list-panel">
            <div class="address-section-heading"><div><span class="panel-kicker">DANH SÁCH CỦA BẠN</span><h2>Địa chỉ đã lưu</h2></div><span class="address-count"><c:out value="${fn:length(addresses)}"/></span></div>
            <c:choose>
                <c:when test="${empty addresses}">
                    <div class="address-empty"><div class="empty-pin" aria-hidden="true">⌖</div><h3>Bạn chưa lưu địa chỉ nào</h3><p>Thêm địa chỉ giao hàng ở biểu mẫu bên cạnh. Địa chỉ đầu tiên sẽ tự động trở thành mặc định.</p></div>
                </c:when>
                <c:otherwise>
                    <div class="address-cards" id="addressCards">
                        <c:forEach var="addr" items="${addresses}">
                            <article class="address-card" data-address-card data-search="${fn:escapeXml(addr.recipientName)} ${fn:escapeXml(addr.recipientPhone)} ${fn:escapeXml(addr.fullLine)}">
                                <div class="address-card-top">
                                    <div class="address-recipient-avatar" aria-hidden="true"><c:out value="${fn:substring(addr.recipientName, 0, 1)}"/></div>
                                    <div class="address-recipient"><h3><c:out value="${addr.recipientName}"/></h3><p><span aria-hidden="true">☎</span> <c:out value="${addr.recipientPhone}"/></p></div>
                                    <c:if test="${addr.defaultAddress}"><span class="default-address-badge"><span></span> Mặc định</span></c:if>
                                </div>
                                <div class="address-card-location"><span aria-hidden="true">⌖</span><p><c:out value="${addr.fullLine}"/></p></div>
                                <div class="address-card-actions">
                                    <button class="account-outline-button compact" type="button" data-edit-toggle="${addr.addressId}" aria-expanded="false">Chỉnh sửa</button>
                                    <c:if test="${!addr.defaultAddress}">
                                        <form method="post" action="${pageContext.request.contextPath}/addresses">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="setDefault">
                                            <input type="hidden" name="addressId" value="${addr.addressId}">
                                            <button class="address-text-button" type="submit">Đặt làm mặc định</button>
                                        </form>
                                    </c:if>
                                    <form method="post" action="${pageContext.request.contextPath}/addresses" data-confirm="Bạn có chắc muốn xóa địa chỉ này không?">
                                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="addressId" value="${addr.addressId}">
                                        <button class="address-delete-button" type="submit">Xóa</button>
                                    </form>
                                </div>
                                <form class="address-edit-form" data-edit-form="${addr.addressId}" method="post" action="${pageContext.request.contextPath}/addresses" hidden>
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="addressId" value="${addr.addressId}">
                                    <div class="address-form-grid">
                                        <label for="editName-${addr.addressId}">Tên người nhận<input id="editName-${addr.addressId}" type="text" name="recipientName" value="<c:out value='${addr.recipientName}'/>" maxlength="120" required></label>
                                        <label for="editPhone-${addr.addressId}">Số điện thoại<input id="editPhone-${addr.addressId}" type="tel" name="recipientPhone" value="<c:out value='${addr.recipientPhone}'/>" pattern="[0-9]{9,12}" inputmode="numeric" maxlength="12" required></label>
                                        <label for="editProvince-${addr.addressId}">Tỉnh / Thành phố<input id="editProvince-${addr.addressId}" type="text" name="province" value="<c:out value='${addr.province}'/>" maxlength="100" required></label>
                                        <label for="editDistrict-${addr.addressId}">Quận / Huyện<input id="editDistrict-${addr.addressId}" type="text" name="district" value="<c:out value='${addr.district}'/>" maxlength="100" required></label>
                                        <label class="address-field-wide" for="editLine-${addr.addressId}">Địa chỉ cụ thể<input id="editLine-${addr.addressId}" type="text" name="addressLine" value="<c:out value='${addr.addressLine}'/>" maxlength="255" required></label>
                                    </div>
                                    <label class="default-checkbox"><input type="checkbox" name="defaultAddress" value="true" <c:if test="${addr.defaultAddress}">checked</c:if>> Đặt làm địa chỉ mặc định</label>
                                    <div class="address-form-actions"><button class="btn-primary" type="submit">Lưu địa chỉ</button><button class="account-quiet-button" type="button" data-edit-cancel="${addr.addressId}">Hủy</button></div>
                                </form>
                            </article>
                        </c:forEach>
                    </div>
                    <div class="address-no-results" id="addressNoResults" hidden>Không tìm thấy địa chỉ phù hợp với từ khóa.</div>
                </c:otherwise>
            </c:choose>
        </section>

        <aside class="address-add-panel">
            <div class="address-section-heading"><div><span class="panel-kicker">ĐỊA CHỈ MỚI</span><h2>Thêm địa chỉ</h2></div><span class="panel-icon" aria-hidden="true">＋</span></div>
            <p class="address-form-intro">Nhập đầy đủ thông tin để đơn hàng được giao đúng nơi nhận.</p>
            <form class="stack-form address-add-form" method="post" action="${pageContext.request.contextPath}/addresses">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="add">
                <label for="recipientName">Tên người nhận <span class="required-mark">*</span><input id="recipientName" type="text" name="recipientName" maxlength="120" autocomplete="name" required></label>
                <label for="recipientPhone">Số điện thoại <span class="required-mark">*</span><input id="recipientPhone" type="tel" name="recipientPhone" pattern="[0-9]{9,12}" inputmode="numeric" maxlength="12" autocomplete="tel" placeholder="0912345678" required></label>
                <div class="address-form-grid">
                    <label for="province">Tỉnh / Thành phố <span class="required-mark">*</span><input id="province" type="text" name="province" maxlength="100" autocomplete="address-level1" required></label>
                    <label for="district">Quận / Huyện <span class="required-mark">*</span><input id="district" type="text" name="district" maxlength="100" autocomplete="address-level2" required></label>
                </div>
                <label for="addressLine">Địa chỉ cụ thể <span class="required-mark">*</span><input id="addressLine" type="text" name="addressLine" maxlength="255" autocomplete="street-address" placeholder="Số nhà, tên đường, phường/xã..." required></label>
                <label class="default-checkbox"><input type="checkbox" name="defaultAddress" value="true" <c:if test="${empty addresses}">checked</c:if>> Đặt làm địa chỉ mặc định</label>
                <button class="btn-primary address-submit" type="submit">＋ Thêm địa chỉ</button>
            </form>
            <div class="address-privacy-note"><span aria-hidden="true">✓</span><p><b>Thông tin riêng tư</b><br>Địa chỉ của bạn được gắn với tài khoản hiện tại.</p></div>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
