<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Xác thực đăng nhập | PrintKraft 3D</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap">

    <style>
        /* =====================================================
           PrintKraft 3D — Login OTP page (self-contained CSS)
           Không phụ thuộc login.css hay bất kỳ file ngoài nào.
           ===================================================== */

        *,
        *::before,
        *::after { box-sizing: border-box; }

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
                radial-gradient(900px 600px at 0% 0%,    rgba(108, 92, 231, .14), transparent 65%),
                radial-gradient(750px 500px at 100% 100%, rgba(255, 107, 107, .10), transparent 65%),
                radial-gradient(700px 500px at 50% 0%,   rgba(0, 184, 148, .06),  transparent 65%);
            background-attachment: fixed;
            -webkit-font-smoothing: antialiased;
        }

        a {
            color: #6C5CE7;
            text-decoration: none;
        }
        a:hover { color: #4A3BC0; }

        /* =============== KHUNG 2 CỘT =============== */
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

        /* =============== CỘT TRÁI — BRAND =============== */
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
        .brand-logo span { color: #C6D8FF; }

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
        .brand-content h1 span { color: #C6D8FF; }

        .brand-desc {
            margin: 20px 0 0;
            color: #E0E8FF;
            font-size: 14px;
            line-height: 1.8;
        }

        /* =============== CỘT PHẢI — FORM =============== */
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
        .back-link:hover { color: #6C5CE7 !important; }

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
            line-height: 1.6;
        }
        .card-subtitle strong {
            color: #1B1B2F;
            font-weight: 700;
            word-break: break-all;
        }

        /* =============== ALERTS =============== */
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

        /* =============== FORM =============== */
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
        .form-field input::placeholder { color: #A0A8B8; }
        .form-field input:focus {
            border-color: #8DA6FF;
            box-shadow: 0 0 0 3px rgba(75, 112, 230, .1);
        }

        /* Ô nhập OTP — chữ to, giãn cách đều */
        .otp-input {
            text-align: center !important;
            font-size: 22px !important;
            letter-spacing: 10px !important;
            font-weight: 700 !important;
            padding: 12px 14px !important;
            min-height: 54px !important;
            font-family: 'Space Grotesk', monospace !important;
        }

        /* =============== BUTTONS =============== */
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
        .btn-primary:active { transform: translateY(0); }

        .link {
            color: #6C5CE7 !important;
            font-size: 12px;
            font-weight: 700;
            text-decoration: none !important;
        }
        .link:hover { color: #4A3BC0 !important; }

        .resend-form {
            margin-top: 16px;
            text-align: center;
        }
        .resend-btn {
            background: none;
            border: 0;
            cursor: pointer;
            padding: 8px;
            font-family: inherit;
            color: #6C5CE7 !important;
            font-size: 13px;
            font-weight: 700;
        }
        .resend-btn:hover { color: #4A3BC0 !important; }

        /* =============== DEMO BOX =============== */
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
        .demo-box code {
            padding: 1px 6px;
            border-radius: 4px;
            background: #EFF1F5;
            color: #4A4E5E;
            font-family: 'Courier New', monospace;
            font-size: 11px;
            font-weight: 700;
        }

        /* =============== RESPONSIVE =============== */
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
            .brand-content h1 { font-size: 30px; }
            .login-card { padding: 32px 30px; }
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
            .brand-content h1 { font-size: 24px; }
            .brand-desc { font-size: 12.5px; }
            .login-card { padding: 24px 20px; }
            .card-title { font-size: 22px; }
            .otp-input {
                font-size: 20px !important;
                letter-spacing: 8px !important;
            }
        }
    </style>
</head>
<body>

<div class="login-shell">

    <!-- ============== CỘT TRÁI: BRAND ============== -->
    <aside class="login-brand">
        <a class="brand-logo" href="${pageContext.request.contextPath}/home">
            PrintKraft <span>3D</span>
        </a>
        <div class="brand-shape" aria-hidden="true">◇</div>
        <div class="brand-content">
            <p class="brand-eyebrow">BẢO MẬT ĐĂNG NHẬP</p>
            <h1>Xác thực<br><span>hai lớp.</span></h1>
            <p class="brand-desc">
                Mỗi lần đăng nhập, PrintKraft gửi một mã 6 chữ số đến email của bạn.
                Nhập mã để hoàn tất đăng nhập an toàn.
            </p>
        </div>
    </aside>

    <!-- ============== CỘT PHẢI: FORM OTP ============== -->
    <main class="login-card">
        <a class="back-link" href="${pageContext.request.contextPath}/login">← Quay lại đăng nhập</a>
        <h2 class="card-title">Nhập mã xác thực</h2>
        <p class="card-subtitle">
            Mã OTP đã được gửi đến <strong><c:out value="${email}"/></strong>.
            Mã có hiệu lực 5 phút.
        </p>

        <c:if test="${not empty error}">
            <div class="alert is-error" role="alert"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert is-success" role="status"><c:out value="${success}"/></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login/otp" class="login-form">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="action" value="verify">

            <label class="form-field">
                <span class="field-label">Mã OTP 6 chữ số</span>
                <input class="otp-input"
                       name="otp" inputmode="numeric"
                       pattern="[0-9]{6}" minlength="6" maxlength="6"
                       autocomplete="one-time-code" required autofocus
                       placeholder="••••••">
            </label>

            <button type="submit" class="btn-primary">Xác nhận đăng nhập</button>
        </form>

        <form method="post" action="${pageContext.request.contextPath}/login/otp" class="resend-form">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="action" value="resend">
            <button type="submit" class="resend-btn">Gửi lại mã xác thực</button>
        </form>

        <div class="demo-box">
            <strong>Môi trường demo</strong>
            Mã OTP demo là <code>111111</code> — nhập để bỏ qua và đăng nhập ngay.
        </div>
    </main>
</div>

</body>
</html>