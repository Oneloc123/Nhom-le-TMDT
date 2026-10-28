<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tài khoản của tôi — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <script src="${pageContext.request.contextPath}/js/account.js" defer></script>
</head>
<body class="auth-body account-body">
<div class="profile-shell account-shell">
    <header class="profile-topbar account-topbar">
        <a class="brand" href="${pageContext.request.contextPath}/home"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <nav aria-label="Điều hướng tài khoản">
            <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
            <a class="active" href="${pageContext.request.contextPath}/profile">Hồ sơ</a>
            <a href="${pageContext.request.contextPath}/addresses">Sổ địa chỉ</a>
            <a href="${pageContext.request.contextPath}/purchase-history">Đơn hàng</a>
            <form class="logout-form" method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <button type="submit" class="topbar-logout">Đăng xuất</button>
            </form>
        </nav>
    </header>

    <main class="profile-main account-main">
        <div class="account-breadcrumb"><a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span><span>Tài khoản của tôi</span></div>

        <section class="account-welcome">
            <div class="account-avatar" aria-hidden="true"><c:out value="${user.userInitial}"/></div>
            <div class="account-welcome-copy">
                <span class="account-overline">KHU VỰC KHÁCH HÀNG</span>
                <h1>Xin chào, <c:out value="${user.displayName}"/>!</h1>
                <p>Quản lý hồ sơ, bảo mật tài khoản và thông tin giao nhận của bạn tại một nơi.</p>
                <div class="account-identity">
                    <span><span class="identity-dot"></span><c:out value="${user.email}"/></span>
                    <span class="status-pill ${user.status == 'ACTIVE' ? 'is-active' : 'is-other'}">
                        <c:choose>
                            <c:when test="${user.status == 'ACTIVE'}">Tài khoản hoạt động</c:when>
                            <c:when test="${user.status == 'LOCKED'}">Tài khoản bị khóa</c:when>
                            <c:otherwise>Chờ kích hoạt</c:otherwise>
                        </c:choose>
                    </span>
                </div>
            </div>
            <a class="account-hero-action" href="${pageContext.request.contextPath}/addresses">Quản lý địa chỉ <span aria-hidden="true">→</span></a>
        </section>

        <c:if test="${not empty message}">
            <div class="alert alert-ok" role="status"><c:out value="${message}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-err" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <div class="account-grid">
            <aside class="account-nav-card" aria-label="Menu tài khoản">
                <div class="account-nav-title">Tài khoản</div>
                <a class="account-nav-link is-current" href="${pageContext.request.contextPath}/profile"><span class="nav-symbol">◉</span> Hồ sơ cá nhân</a>
                <a class="account-nav-link" href="${pageContext.request.contextPath}/addresses"><span class="nav-symbol">⌖</span> Sổ địa chỉ <span class="nav-count"><c:out value="${fn:length(addresses)}"/></span></a>
                <a class="account-nav-link" href="${pageContext.request.contextPath}/purchase-history"><span class="nav-symbol">▤</span> Lịch sử đơn hàng</a>
                <a class="account-nav-link" href="${pageContext.request.contextPath}/favorites"><span class="nav-symbol">♡</span> Sản phẩm yêu thích</a>
                <a class="account-nav-link" href="${pageContext.request.contextPath}/quotes"><span class="nav-symbol">₫</span> Yêu cầu báo giá</a>
                <div class="account-nav-help">
                    <div class="help-icon" aria-hidden="true">?</div>
                    <b>Cần hỗ trợ?</b>
                    <p>Hãy kiểm tra thông tin liên hệ để việc tư vấn và giao hàng thuận tiện hơn.</p>
                    <a href="${pageContext.request.contextPath}/home">Khám phá PrintKraft →</a>
                </div>
            </aside>

            <div class="account-content">
                <section class="panel account-panel">
                    <div class="panel-head account-panel-head">
                        <div><span class="panel-kicker">THÔNG TIN CƠ BẢN</span><h2>Hồ sơ cá nhân</h2><p>Cập nhật tên hiển thị và số điện thoại liên hệ.</p></div>
                        <span class="panel-icon" aria-hidden="true">✎</span>
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/profile" class="stack-form account-form">
                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                        <input type="hidden" name="action" value="info">
                        <div class="account-form-grid">
                            <label for="fullName">Họ và tên <span class="required-mark">*</span>
                                <input id="fullName" type="text" name="fullName" value="<c:out value='${user.fullName}'/>" autocomplete="name" maxlength="120" required>
                            </label>
                            <label for="phone">Số điện thoại
                                <input id="phone" type="tel" name="phone" value="<c:out value='${user.phone}'/>" autocomplete="tel" inputmode="numeric" pattern="[0-9]{9,12}" maxlength="12" placeholder="Ví dụ: 0912345678" aria-describedby="phone-help">
                                <small id="phone-help" class="field-help">Nhập từ 9 đến 12 chữ số, không cần dấu cách.</small>
                            </label>
                            <label class="field-span-2" for="email">Email đăng nhập
                                <input id="email" type="email" value="<c:out value='${user.email}'/>" disabled>
                                <small class="field-help">Email hiện là định danh tài khoản và chưa thể thay đổi tại đây.</small>
                            </label>
                        </div>
                        <div class="account-form-actions"><button type="submit" class="btn-primary">Lưu thay đổi <span aria-hidden="true">→</span></button></div>
                    </form>
                </section>

                <section class="panel account-panel">
                    <div class="panel-head account-panel-head">
                        <div><span class="panel-kicker">BẢO MẬT</span><h2>Đổi mật khẩu</h2><p>Sử dụng mật khẩu riêng và không chia sẻ với người khác.</p></div>
                        <span class="panel-icon security-icon" aria-hidden="true">⌑</span>
                    </div>
                    <c:choose>
                        <c:when test="${user.provider == 'google' || user.provider == 'facebook'}">
                            <div class="security-notice"><b>Tài khoản đăng nhập bằng mạng xã hội</b><p>Bạn đang dùng nhà cung cấp đăng nhập bên ngoài. Hãy quản lý mật khẩu trong tài khoản Google hoặc Facebook tương ứng.</p></div>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/profile" class="stack-form account-form password-form" data-password-form>
                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                <input type="hidden" name="action" value="password">
                                <label for="oldPassword">Mật khẩu hiện tại
                                    <span class="password-control"><input id="oldPassword" type="password" name="oldPassword" autocomplete="current-password" required><button type="button" class="password-toggle" data-password-toggle="oldPassword" aria-label="Hiện mật khẩu hiện tại">Hiện</button></span>
                                </label>
                                <div class="account-form-grid">
                                    <label for="newPassword">Mật khẩu mới
                                        <span class="password-control"><input id="newPassword" type="password" name="newPassword" minlength="8" autocomplete="new-password" placeholder="Ít nhất 8 ký tự" required data-password-strength><button type="button" class="password-toggle" data-password-toggle="newPassword" aria-label="Hiện mật khẩu mới">Hiện</button></span>
                                    </label>
                                    <label for="confirmPassword">Xác nhận mật khẩu mới
                                        <span class="password-control"><input id="confirmPassword" type="password" name="confirmPassword" minlength="8" autocomplete="new-password" placeholder="Nhập lại mật khẩu mới" required data-password-confirm><button type="button" class="password-toggle" data-password-toggle="confirmPassword" aria-label="Hiện xác nhận mật khẩu">Hiện</button></span>
                                    </label>
                                </div>
                                <div class="password-strength" aria-live="polite"><div class="strength-track"><span data-strength-bar></span></div><span data-strength-label>Độ mạnh mật khẩu sẽ hiển thị tại đây.</span></div>
                                <p class="field-help">Nên dùng ít nhất 8 ký tự, kết hợp chữ hoa, chữ thường, số hoặc ký tự đặc biệt.</p>
                                <div class="account-form-actions"><button type="submit" class="btn-primary">Cập nhật mật khẩu <span aria-hidden="true">→</span></button></div>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section class="panel account-panel account-security-summary">
                    <div class="panel-head account-panel-head"><div><span class="panel-kicker">TỔNG QUAN</span><h2>Thông tin tài khoản</h2></div></div>
                    <dl class="account-details">
                        <div><dt>Mã khách hàng</dt><dd>#<c:out value="${user.id}"/></dd></div>
                        <div><dt>Ngày tham gia</dt><dd><c:out value="${user.createdAt}"/></dd></div>
                        <div><dt>Phương thức đăng nhập</dt><dd><c:choose><c:when test="${user.provider == 'google'}">Google</c:when><c:when test="${user.provider == 'facebook'}">Facebook</c:when><c:otherwise>Email và mật khẩu</c:otherwise></c:choose></dd></div>
                        <div><dt>Sổ địa chỉ</dt><dd><c:out value="${fn:length(addresses)}"/> địa chỉ <a href="${pageContext.request.contextPath}/addresses">Quản lý</a></dd></div>
                    </dl>
                </section>
            </div>
        </div>
    </main>
</div>
</body>
</html>
