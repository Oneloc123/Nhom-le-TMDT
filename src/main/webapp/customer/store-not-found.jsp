<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Không thể mở nhà cung cấp | PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css?v=20261125-modern1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customer-modern.css?v=20261125-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/supplier-directory.css?v=20261125-1">
    <style>
        .supplier-error-wrap {
            width: min(650px, calc(100% - 32px));
            margin: clamp(38px, 8vh, 90px) auto;
        }
        .supplier-error-card {
            padding: clamp(25px, 5vw, 44px);
            border: 1px solid #E4E6F4;
            border-radius: 24px;
            background: linear-gradient(145deg, #FFFFFF, #F7F6FF);
            box-shadow: 0 18px 44px rgba(37, 43, 82, .08);
            text-align: center;
        }
        .supplier-error-icon {
            display: grid;
            place-items: center;
            width: 62px;
            height: 62px;
            margin: 0 auto 18px;
            border-radius: 20px;
            background: #EEECFF;
            color: #5143C7;
            font-size: 27px;
            font-weight: 800;
        }
        .supplier-error-card h1 {
            margin: 0;
            color: #20243A;
            font-size: clamp(24px, 4vw, 32px);
            letter-spacing: -.035em;
        }
        .supplier-error-card p {
            margin: 12px 0 0;
            color: #68718A;
            font-size: 14px;
            line-height: 1.8;
        }
        .supplier-error-actions {
            display: flex;
            justify-content: center;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 24px;
        }
        .supplier-error-actions a {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-height: 43px;
            padding: 10px 15px;
            border-radius: 12px;
            font-size: 13px;
            font-weight: 800;
            text-decoration: none;
        }
        .supplier-error-actions .primary {
            background: linear-gradient(135deg, #6C5CE7, #8276ED);
            color: #fff;
        }
        .supplier-error-actions .secondary {
            border: 1px solid #DDE1F0;
            background: #fff;
            color: #5143C7;
        }
    </style>
</head>
<body>
<%@ include file="header.jsp" %>
<main class="supplier-error-wrap">
    <section class="supplier-error-card">
        <div class="supplier-error-icon" aria-hidden="true">!</div>
        <h1><c:out value="${supplierErrorTitle}"/></h1>
        <p><c:out value="${supplierErrorMessage}"/></p>
        <div class="supplier-error-actions">
            <a class="primary" href="${pageContext.request.contextPath}/stores">Quay lại nhà cung cấp</a>
            <a class="secondary" href="${pageContext.request.contextPath}/home">Về trang chủ</a>
        </div>
    </section>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
