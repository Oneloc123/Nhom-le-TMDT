<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Đăng nhập | PrintKraft 3D</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap">

    <style>
        /* =====================================================
           PrintKraft 3D — Login page styles (self-contained)
           Không phụ thuộc user-v2.css hay bất kỳ file nào khác.
           ===================================================== */

        *,
        *::before,
        *::after {
            box-sizing: border-box;
        }

        html, body {
            margin: 0;
            padding: 0;
            min-height: 100%;
        }

        body {
            font-family: 'IBM Plex Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
            color: #1B1B2F;
            font-size: 14px;
            line-height: 1.5;
            background-color: #FDF9F3;
            background-image:
                radial-gradient(900px 600px at 0% 0%,   rgba(108, 92, 231, .14), transparent 65%),
                radial-gradient(750px 500px at 100% 100%, rgba(255, 107, 107, .10), transparent 65%),
                radial-gradient(700px 500px at 50% 0%,   rgba(0, 184, 148, .06),  transparent 65%);
            background-attachment: fixed;
            -webkit-font-smoothing: antialiased;
        }

        a {
            color: #6C5CE7;
            text-decoration: none;
        }
        a:hover {
            color: #4A3BC0;
        }

        /* =====================================================
           KHUNG 2 CỘT
           ===================================================== */
        .login-shell {
            display: grid;
            grid-template-columns: minmax(0, 1fr) minmax(380px, 460px);
            width: min(1080px, calc(100% - 40px));
            min-height: 640px;
            margin: 3vh auto;
            overflow: hidden;
            border-radius: 24px;
            background: #fff;
            box-shadow: 0 30px 80px rgba(31, 52, 103, .12);
        }

        /* =====================================================
           CỘT TRÁI — BRAND
           ===================================================== */
        .login-brand {
            position: relative;
            display: flex;
            flex-direction: column;
            justify-content: center;
            overflow: hidden;
            padding: 56px;
            color: #fff;
            background: linear-gradient(145deg, #30215F 0%, #6C5CE7 60%, #A29BFE 100%);
        }

        .brand-logo {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            position: absolute;
            top: 40px;
            left: 56px;
            color: #fff !important;
            font-family: 'Space Grotesk', sans-serif;
            font-size: 20px;
            font-weight: 700;
            letter-spacing: -.04em;
            text-decoration: none !important;
        }
        .brand-logo span {
            color: #C6D8FF;
        }

        .brand-shape {
            position: absolute;
            top: 90px;
            right: 40px;
            color: rgba(255, 255, 255, .08);
            font-size: 280px;
            line-height: 1;
            pointer-events: none;
            user-select: none;
        }

        .brand-content {
            position: relative;
            z-index: 1;
            max-width: 440px;
        }

        .brand-eyebrow {
            margin: 0 0 14px;
            color: #C8D6FF;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: .15em;
            text-transform: uppercase;
        }

        .brand-content h1 {
            margin: 0;
            font-family: 'Space Grotesk', sans-serif;
            font-size: clamp(32px, 4vw, 44px);
            line-height: 1.15;
            letter-spacing: -.04em;
            font-weight: 700;
        }
        .brand-content h1 span {
            color: #C6D8FF;
        }

        .brand-desc {
            margin: 20px 0 0;
            color: #E0E8FF;
            font-size: 14px;
            line-height: 1.8;
        }

        /* =====================================================
           CỘT PHẢI — FORM
           ===================================================== */
        .login-card {
            display: flex;
            flex-direction: column;
            justify-content: center;
            padding: 48px 44px;
            background: #fff;
        }

        .back-link {
            display: inline-block;
            margin-bottom: 22px;
            color: #6c7890 !important;
            font-size: 12px;
            font-weight: 700;
            text-decoration: none !important;
        }
        .back-link:hover {
            color: #6C5CE7 !important;
        }

        .card-title {
            margin: 0 0 8px;
            font-family: 'Space Grotesk', sans-serif;
            font-size: 28px;
            letter-spacing: -.04em;
            font-weight: 700;
            color: #1B1B2F;
        }

        .card-subtitle {
            margin: 0 0 22px;
            color: #686880;
            font-size: 13px;
        }

        /* =====================================================
           ALERTS
           ===================================================== */
        .alert {
            padding: 12px 16px;
            margin: 0 0 14px;
            border-radius: 10px;
            border: 1px solid;
            font-size: 13px;
            line-height: 1.5;
        }
        .alert.is-error {
            border-color: #F1CBD0;
            background: #FFF4F5;
            color: #A63B4D;
        }
        .alert.is-success {
            border-color: #CAEADD;
            background: #F0FBF5;
            color: #17784F;
        }

        /* =====================================================
           SOCIAL BUTTONS
           ===================================================== */
        .social-group {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin: 4px 0 4px;
        }

        .social-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            width: 100%;
            min-height: 46px;
            padding: 10px 16px;
            border: 1.5px solid #D9DBE0;
            border-radius: 11px;
            background: #fff;
            color: #1B1B2F !important;
            font-family: inherit;
            font-size: 14px;
            font-weight: 600;
            line-height: 1.2;
            text-decoration: none !important;
            cursor: pointer;
            transition: background .15s ease, border-color .15s ease, transform .05s ease;
        }
        .social-btn:hover {
            background: #F6F7F9;
            border-color: #B9BDC4;
            color: #1B1B2F !important;
        }
        .social-btn:active {
            transform: translateY(1px);
        }

        .social-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 20px;
            height: 20px;
            flex: 0 0 20px;
            overflow: hidden;
            line-height: 0;
        }

        .social-icon > svg {
            display: block;
            width: 20px;
            height: 20px;
        }

        .social-label {
            white-space: nowrap;
        }

        /* =====================================================
           DIVIDER
           ===================================================== */
        .divider {
            display: flex;
            align-items: center;
            gap: 12px;
            margin: 18px 0;
            color: #8A8F98;
            font-size: 11px;
            font-weight: 600;
            letter-spacing: .06em;
            text-transform: uppercase;
        }
        .divider::before,
        .divider::after {
            content: "";
            flex: 1 1 auto;
            height: 1px;
            background: #E3E5E9;
        }

        /* =====================================================
           FORM
           ===================================================== */
        .login-form {
            display: flex;
            flex-direction: column;
            gap: 14px;
        }

        .form-field {
            display: flex;
            flex-direction: column;
            gap: 7px;
        }

        .field-label {
            color: #35425F;
            font-size: 12px;
            font-weight: 700;
        }

        .form-field input {
            width: 100%;
            min-height: 44px;
            padding: 10px 14px;
            border: 1px solid #DFE5EF;
            border-radius: 10px;
            background: #fff;
            color: #263451;
            font-family: inherit;
            font-size: 13px;
            outline: none;
            transition: border-color .15s, box-shadow .15s;
        }
        .form-field input::placeholder {
            color: #A0A8B8;
        }
        .form-field input:focus {
            border-color: #8DA6FF;
            box-shadow: 0 0 0 3px rgba(75, 112, 230, .1);
        }

        .form-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
            margin-top: 2px;
        }

        .form-hint {
            color: #98A2B3;
            font-size: 11px;
        }

        .link {
            color: #6C5CE7 !important;
            font-size: 12px;
            font-weight: 700;
            text-decoration: none !important;
        }
        .link:hover {
            color: #4A3BC0 !important;
        }

        /* =====================================================
           PRIMARY BUTTON
           ===================================================== */
        .btn-primary {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            min-height: 46px;
            padding: 12px 20px;
            margin-top: 6px;
            border: 0;
            border-radius: 11px;
            color: #fff;
            background: linear-gradient(135deg, #6C5CE7, #A29BFE);
            font-family: inherit;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            box-shadow: 0 8px 20px rgba(108, 92, 231, .25);
            transition: box-shadow .15s, transform .15s;
        }
        .btn-primary:hover {
            box-shadow: 0 12px 26px rgba(108, 92, 231, .32);
            transform: translateY(-1px);
        }
        .btn-primary:active {
            transform: translateY(0);
        }

        /* =====================================================
           SWITCH / DEMO
           ===================================================== */
        .switch-line {
            margin: 20px 0 0;
            color: #7D899E;
            font-size: 13px;
            text-align: center;
        }
        .switch-line .link {
            font-size: 13px;
        }

        .demo-box {
            margin-top: 22px;
            padding: 12px 14px;
            border: 1px dashed #D9DBE0;
            border-radius: 10px;
            background: #FAFBFC;
            color: #6C7080;
            font-size: 11px;
            line-height: 1.7;
        }
        .demo-box strong {
            display: block;
            margin-bottom: 6px;
            color: #4A4E5E;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: .04em;
        }
        .demo-box ul {
            margin: 0;
            padding: 0;
            list-style: none;
        }
        .demo-box code {
            padding: 1px 5px;
            border-radius: 4px;
            background: #EFF1F5;
            color: #4A4E5E;
            font-family: 'Courier New', monospace;
            font-size: 10.5px;
        }

        /* =====================================================
           RESPONSIVE
           ===================================================== */
        @media (max-width: 900px) {
            .login-shell {
                grid-template-columns: 1fr;
                min-height: auto;
                margin: 20px auto;
                border-radius: 18px;
            }

            .login-brand {
                min-height: 260px;
                padding: 40px 30px;
            }

            .brand-logo {
                position: static;
                margin-bottom: 24px;
            }

            .brand-shape {
                top: 10px;
                right: 10px;
                font-size: 200px;
            }

            .brand-content h1 {
                font-size: 30px;
            }

            .login-card {
                padding: 32px 30px;
            }
        }

        @media (max-width: 560px) {
            .login-shell {
                width: calc(100% - 20px);
                margin: 10px auto;
                border-radius: 14px;
            }

            .login-brand {
                padding: 28px 22px;
                min-height: 200px;
            }

            .brand-content h1 {
                font-size: 24px;
            }

            .brand-desc {
                font-size: 12.5px;
            }

            .login-card {
                padding: 24px 20px;
            }

            .card-title {
                font-size: 22px;
            }

            .social-btn {
                font-size: 13px;
                min-height: 44px;
            }
        }
    </style>
</head>
<body>

<div class="login-shell">

    <!-- ============== LEFT: BRAND ============== -->
    <aside class="login-brand">
        <a class="brand-logo" href="${pageContext.request.contextPath}/home">
            PrintKraft <span>3D</span>
        </a>

        <div class="brand-shape" aria-hidden="true">◇</div>

        <div class="brand-content">
            <p class="brand-eyebrow">TÀI KHOẢN PRINTKRAFT</p>
            <h1>
                Sáng tạo mô hình.<br>
                <span>Hiện thực hóa ý tưởng.</span>
            </h1>
            <p class="brand-desc">
                Quản lý đơn hàng, lưu địa chỉ giao hàng và theo dõi các yêu cầu in 3D tại một nơi.
            </p>
        </div>
    </aside>

    <!-- ============== RIGHT: FORM ============== -->
    <main class="login-card">

        <a class="back-link" href="${pageContext.request.contextPath}/home">← Về trang chủ</a>

        <h2 class="card-title">Chào mừng trở lại</h2>
        <p class="card-subtitle">Đăng nhập để tiếp tục với PrintKraft.</p>

        <%-- ===== Alerts ===== --%>
        <c:if test="${not empty error}">
            <div class="alert is-error" role="alert"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty resetOk}">
            <div class="alert is-success"><c:out value="${resetOk}"/></div>
        </c:if>
        <c:if test="${param.registered eq '1'}">
            <div class="alert is-success">Email đã được xác thực. Bạn có thể đăng nhập ngay.</div>
        </c:if>
        <c:if test="${param.loggedOut eq '1'}">
            <div class="alert is-success">Bạn đã đăng xuất thành công.</div>
        </c:if>
        <c:if test="${param.oauth eq 'unavailable'}">
            <div class="alert is-error">
                Đăng nhập mạng xã hội chưa được cấu hình. Vui lòng dùng email và mật khẩu.
            </div>
        </c:if>
        <c:if test="${param.oauth eq 'failed'}">
            <div class="alert is-error">
                <c:out value="${empty param.msg ? 'Đăng nhập mạng xã hội thất bại.' : param.msg}"/>
            </div>
        </c:if>
        <c:if test="${param.otpExpired eq '1'}">
            <div class="alert is-error">Phiên xác thực OTP đã hết hạn. Vui lòng đăng nhập lại.</div>
        </c:if>
        <c:if test="${param.otpLocked eq '1'}">
            <div class="alert is-error">Bạn đã nhập sai quá nhiều lần. Vui lòng đăng nhập lại.</div>
        </c:if>
        <c:if test="${param.error eq 'account'}">
            <div class="alert is-error">Tài khoản không còn hoạt động. Vui lòng liên hệ quản trị viên.</div>
        </c:if>

        <%-- ===== OAuth buttons ===== --%>
        <c:url var="googleUrl" value="/login/google">
            <c:if test="${not empty param.redirect}">
                <c:param name="redirect" value="${param.redirect}"/>
            </c:if>
        </c:url>
        <c:url var="facebookUrl" value="/login/facebook">
            <c:if test="${not empty param.redirect}">
                <c:param name="redirect" value="${param.redirect}"/>
            </c:if>
        </c:url>

        <div class="social-group">
            <a class="social-btn" href="${googleUrl}">
                <span class="social-icon">
                    <svg viewBox="0 0 48 48" width="20" height="20" aria-hidden="true">
                        <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.9 29.3 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34.1 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.2-.1-2.4-.4-3.5z"/>
                        <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34.1 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/>
                        <path fill="#4CAF50" d="M24 44c5.2 0 10-2 13.6-5.2l-6.3-5.3C29.3 35.1 26.8 36 24 36c-5.3 0-9.7-3.1-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z"/>
                        <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.3 4.3-4.1 5.7l6.3 5.3c-.4.4 6.5-4.7 6.5-15 0-1.2-.1-2.4-.4-3.5z"/>
                    </svg>
                </span>
                <span class="social-label">Đăng nhập bằng Google</span>
            </a>

            <a class="social-btn" href="${facebookUrl}">
                <span class="social-icon">
                    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                        <path fill="#1877F2" d="M22 12a10 10 0 1 0-11.6 9.9v-7H7.9V12h2.5V9.8c0-2.5 1.5-3.9 3.8-3.9 1.1 0 2.2.2 2.2.2v2.5h-1.3c-1.2 0-1.6.8-1.6 1.6V12h2.8l-.4 2.9h-2.4v7A10 10 0 0 0 22 12z"/>
                        <path fill="#fff" d="M15.4 14.9 15.8 12h-2.8V10c0-.8.4-1.6 1.6-1.6h1.3V5.9s-1.1-.2-2.2-.2c-2.3 0-3.8 1.4-3.8 3.9V12H7.9v2.9h2.5v7c.5.1 1.1.1 1.6.1s1.1 0 1.6-.1v-7h2.4z"/>
                    </svg>
                </span>
                <span class="social-label">Đăng nhập bằng Facebook</span>
            </a>
        </div>

        <div class="divider"><span>hoặc dùng email</span></div>

        <%-- ===== Email form ===== --%>
        <form method="post" action="${pageContext.request.contextPath}/login" class="login-form">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <c:if test="${not empty param.redirect}">
                <input type="hidden" name="redirect" value="<c:out value='${param.redirect}'/>">
            </c:if>

            <label class="form-field">
                <span class="field-label">Email</span>
                <input type="email" name="email"
                       maxlength="190" autocomplete="username" required
                       value="<c:out value='${not empty email ? email : "demo@printkraft.local"}'/>"
                       placeholder="demo@printkraft.local">
            </label>

            <label class="form-field">
                <span class="field-label">Mật khẩu</span>
                <input type="password" name="password"
                       autocomplete="current-password" required maxlength="72"
                       value="PrintKraft@123"
                       placeholder="PrintKraft@123">
            </label>

            <div class="form-row">
                <span class="form-hint">Không chia sẻ mật khẩu với người khác.</span>
                <a class="link" href="${pageContext.request.contextPath}/forgot-password">Quên mật khẩu?</a>
            </div>

            <button type="submit" class="btn-primary">Đăng nhập</button>
        </form>

        <p class="switch-line">
            Chưa có tài khoản?
            <a class="link" href="${pageContext.request.contextPath}/register">Tạo tài khoản</a>
        </p>

        <div class="demo-box">
            <strong>Tài khoản demo</strong>
            <ul>
                <li>Admin: <code>admin@printkraft.local</code> / <code>Admin@12345</code></li>
                <li>Xưởng: <code>workshop01@example.test</code> / <code>Print3D-Demo-2026!</code></li>
                <li>Khách: <code>demo@printkraft.local</code> / <code>PrintKraft@123</code></li>
            </ul>
        </div>

    </main>
</div>

<jsp:include page="/customer/chatbot-widget.jsp"/>
</body>
</html>